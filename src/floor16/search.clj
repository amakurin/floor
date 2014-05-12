(ns floor16.search
  (:require [compojure.core :refer [defroutes GET]]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [floor16.storage :as db]
            [korma.core :as k]
            [korma.sql.fns :as kf]
            [clj-time.core :as tc]
            [clj-time.format :as tf]
            [clj-time.local :as tl]
            [clojure.tools.reader.edn :as edn]
            [environ.core :refer [env]]
            [clj-rhino :as js]))

(def ps [{:id 0 :mnemo :all-the-time :name "за все время"}
         {:id 30 :mnemo :last-month :name "за месяц"}
         {:id 7 :mnemo :last-week :name "за неделю"}
         {:id 1 :mnemo :last-day :name "за сутки"}])

(def conf {:total-area {:pred range-pred
                        :bounds {:btm 0 :top 200}}
           :living-area {:pred range-pred
                         :bounds {:btm 0 :top 100}}
           :kitchen-area {:pred range-pred
                          :bounds {:btm 0 :top 50}}
           :floor {:pred range-pred
                   :bounds {:btm 0 :top 30}}
           :floors {:pred range-pred
                    :bounds {:btm 0 :top 30}}
           :price {:pred range-pred
                   :bounds {:btm 0 :top 50000}}
           :distance {:pred distance-pred
                      :bounds {:btm 0 :top 60}}
           :toilet {:pred in-pred}
           :metro {:pred in-pred}
           :building-type {:pred in-pred}
           :district {:pred in-pred}
           :appartment-type {:pred appartment-type-pred}
           :with-photo {:pred with-photo-pred}
           :published {:pred published-pred}
           :balcony {:pred balcony-pred}

           :city           {:pred as-is-pred }
           :furniture      {:pred as-is-pred }
           :internet       {:pred as-is-pred }
           :tv             {:pred as-is-pred }
           :frige          {:pred as-is-pred }
           :washer         {:pred as-is-pred }
           :conditioner    {:pred as-is-pred }
           :parking        {:pred as-is-pred }
           :intercom       {:pred as-is-pred }
           :security       {:pred as-is-pred }
           :concierge      {:pred as-is-pred }
           :kids           {:pred as-is-pred }
           :pets           {:pred as-is-pred }
           :not-only-russo {:pred as-is-pred }
           :only-russo     {:pred as-is-pred }
           })

(defn appartment-type-pred [k v]
  (if (or (not (vector? v)) (empty? v)) {}
    (let [v (->>
             (k/select :appartment-types
                       (k/fields :id)
                       (k/where {:mnemo [kf/pred-in (map str v)]}))
             (mapcat (fn [{:keys [id]}] (if (= id 4) [4 5 6 7] [id])))
             vec)]
      {:appartment-type [kf/pred-in v]}
      )))

(defn with-photo-pred [k v]
  (if (true? v)
    {:imgs [kf/pred-like "%jpg%"]}
    {}))

(defn published-pred [k v]
  (if (or (not (number? v)) (<= v 0)) {}
    {:created [kf/pred->= (tf/unparse (tf/formatters :mysql)
                                      (tc/minus (tl/local-now)
                                                (tc/days v)))]}))
(defn balcony-pred [k v]
  (if (true? v)
    (kf/pred-or (kf/pred-= :balcony v)(kf/pred-= :bow-window v)(kf/pred-= :loggia v))
    {}))

(defn in-pred [k v]
  (if (or (not (vector? v)) (empty? v)) {}
    {k [kf/pred-in v]}))

(defn range-pred [k {:keys [btm top] :as v} bounds]
  (cond (or (not (number? btm))
          (not (number? top))
          (= v bounds)) {}
        (= top (:top bounds)) {k [kf/pred->= btm]}
        (= btm (:btm bounds)) {k [kf/pred-<= top]}
        :else {k [kf/pred-between [btm top]]}))

(defn min->meter-walk[dist]
  (quot(* dist 5000)60))

(defn meter->min-walk[dist]
  (quot(* dist 60)5000))

(defn distance-pred [k {:keys [btm top]:as v} bounds]
  (if (or (not (number? btm))
          (not (number? top))) {}
    (let [v {:btm (min->meter-walk btm) :top (min->meter-walk top)}
          bounds {:btm (min->meter-walk (:btm bounds)) :top (min->meter-walk (:top bounds))}]
      (range-pred k v bounds))))

(defn as-is-pred [k v]
  (if (or (number? v)(string? v)(false? v)(true? v))
    {k v}
    {}))

(defn get-search-settings []
  (->> conf
       (filter #(> (-> % val count) 1))
       (map (fn [[k v]] [k (:bounds v)]))
       (into {})
       (merge {:published 0
               :appartment-type []
               :toilet []
               :building-type []
               :metro []
               :district []})))

(defn convert-russo [{:keys [only-russo not-only-russo] :as q}]
  (let [only-russo (true? only-russo)
        not-only-russo (true? not-only-russo)]
    (cond (and only-russo (not not-only-russo)) {:only-russo true}
          (and (not only-russo) not-only-russo) (kf/pred-or {:only-russo false} {:only-russo nil})
          :else {})))

(defn convert-qe [[k v] conf]
  (when-let [{:keys [pred bounds]} (k conf)]
    (if bounds
      (pred k v bounds)
      (pred k v))))

(defn convert-metdis [q conf]
  (let [qconv (->> (select-keys q [:metro :district])
                   (remove (fn [[k v]] (empty? v)))
                   (map #(convert-qe % conf)))
        cnt (count qconv)]
    (cond (= 0 cnt) {}
          (= 1 cnt) (first qconv)
          :else (apply kf/pred-or qconv))))

(defn default-converter [q conf]
  (let [qconv (->> (dissoc q :not-only-russo :only-russo :metro :district)
                   (map #(convert-qe % conf)))
        metdis [(convert-metdis q conf)]
        russo [(convert-russo q)]]
    (remove empty? (concat qconv metdis russo))))

(defn page->offset [page limit]
  (let [page (or page 0)]
    (* page limit)))

(defn or-func [v & preds]
  (some true? (map #(% v) preds)))

(defn exclude-fields[m & preds]
  (->> m
       (remove #(apply or-func % preds))
       (into {})))

(defn val-nil? []
  #(-> % second nil?))

(defn key-is? [k]
  #(= k (first %)))

(defn post-process[{:keys [imgs distance] :as m}]
  (merge m {:distance (when distance (meter->min-walk distance))
            :imgs (when imgs (edn/read-string imgs))}))

(defn try-parse-int [s]
  (if (number? s) s
    (try
      (Integer. s)
      (catch NumberFormatException e nil))))

(defn select-resource [entity & [{:keys [query q-convert exclude-field-preds post-process page limit]
                                  :or {q-convert identity
                                       post-process identity
                                       exclude-field-preds [(val-nil?)]}}]]
  (let [query (or query {})
        page (dec (or (try-parse-int page) 1))
        limit (or limit (env :default-select-limit))
        offset (page->offset page limit)
        condition (q-convert query)
        cnt (->>
             (-> (k/select* entity)
                (k/aggregate (kf/agg-count :*) :cnt)
                (k/where (if (map? condition) condition (apply kf/pred-and condition)))
                (k/exec))
             first :cnt)]
    {:total cnt
     :offset offset
     :limit limit
     :items
     (->>
      (-> (k/select* entity)
          (k/where (if (map? condition) condition (apply kf/pred-and condition)))
          (k/offset offset)
          (k/limit limit)
          (k/exec))
      (map post-process)
      (map #(apply exclude-fields % exclude-field-preds))
      vec
      )}))

(defn search [q & [page limit]]
  (select-resource :pub {:query q :page page :limit limit
                         :q-convert #(default-converter % conf)
                         :post-process post-process
                         :exclude-field-preds [(val-nil?)(key-is? :phone)(key-is? :id)(key-is? :src-date)]}))


(defn gen-validate [raw-query]
  (let [allowed-pattern #"[a-zA-Z][a-zA-Z\-\d\?]+"
        allowed-predicate #(or (string? %) (number? %) (true? %) (false? %) (vector? %) (keyword? %) (map? %))]
    (reduce (fn [r [k v]]
              (let [match (re-matches allowed-pattern (name k))
                    kw (when match (keyword match))]
                (if (and kw (allowed-predicate v)) (assoc r kw v) r)))
            {} raw-query)))

(defn validate-query [q & [rules]]
  (if-not rules
    (gen-validate q)
    (reduce
     (fn [r [k v]]
       (let [kw (keyword k)
             rule (kw rules)]
         (if (and rule (rule v)) (assoc r kw v) r)))
     {} q)))

(defn default-city [req] 0)

(defn empty-query [req]
  {:city (default-city req)})

(defn decode-query [qstr & [req]]
  (let [q (try (edn/read-string qstr)
            (catch Exception err
              (println "Error while parsing query string: " err)(empty-query req)))]
    (merge (empty-query req)(validate-query q))))

(defn try-decode-query [qstr]
  (try (edn/read-string qstr)
    (catch Exception err
      (println "Error while parsing query string: " err)nil)))
