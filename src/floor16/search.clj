(ns floor16.search
  (:require [compojure.core :refer [defroutes GET]]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [floor16.storage :as db]
            [clojure.string :as s]
            [korma.core :as k]
            [korma.sql.fns :as kf]
            [clj-time.coerce :as tco]
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

(def os [{:id 0 :mnemo [:pub.created :DESC] :name "дате публикации"}
         {:id 1 :mnemo [:price :ASC] :name "цене жилья"}])

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

           :seoid          {:pred as-is-pred }
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
(defn default-city [req] 0)

(defn get-search-settings [req]
  (->> conf
       (filter #(> (-> % val count) 1))
       (map (fn [[k v]] [k (:bounds v)]))
       (into {})
       (merge {:city (default-city req)
               :published 0
               :order 0
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

(defn prepare-description [desc]
  (-> desc
   (s/replace #"[\n\r?]+" " ")
   (s/replace #"\s+" " ")
   (s/replace #"\s+" " ")
   (s/replace
    #"(?iux)
    (?<![\d])
    (?: \b(?: мобильный|моб|сотовый|сот)[\.\:]?\s?)?
    (?: \b(?: телефон|тел|т)[\.\:]?\s?)?
    (?: \+\s?)?
    (?: [78][\.\-\s]?)?
    (?: \(\d{3}\)\s?|(?: \d\s?[\.\-]?\s?){3})
    (?: (?: \d\s?[\.\-\s]?\s?){6,7}|(?: \d\s?[\.\-\s]?\s?){4})
    \b(?![\d])" "")
   (s/replace #"[\.\s\,]+$" ".")))

(defn same-date? [d1 d2]
  (and (= (tc/year d1)(tc/year d2))
       (= (tc/month d1)(tc/month d2))
       (= (tc/day d1)(tc/day d2))))

(defn post-process[{:keys [imgs distance description thumb address created lat lng appartment-type-mnemo] :as m} include]
  (-> m
      (#(if distance
          (assoc % :distance (max 1 (meter->min-walk distance)))
          %))

      (#(if (and (include :imgs) imgs)
          (assoc % :imgs (->>(edn/read-string imgs)
                             (map (fn [x] (str (env :img-server-url) x)))
                             vec))
          (dissoc % :imgs)))

      (#(if (and (include :imgs-cnt) imgs)
          (assoc % :imgs-cnt (count(edn/read-string imgs)))
          (dissoc % :imgs-cnt)))

      (#(if (and (include :thumb) thumb)
          (assoc % :thumb (str (env :img-server-url) thumb))
          (dissoc % :thumb)))

      (#(if (and (include :description) description)
          (assoc % :description (prepare-description description))
          (dissoc % :description)))

      (#(if address
          (assoc % :address (s/replace address #"[\s\.\,_\-]+$" ""))
          %))

      (#(if (and (include :lat-lng) lat lng)
          (merge % {:lat (Float. lat) :lng (Float. lng)})
          (dissoc % :lat :lng)))

      (#(if (and (include :appartment-type-mnemo) appartment-type-mnemo)
          (merge % {:appartment-type-mnemo (let [app-type (read-string appartment-type-mnemo)]
                                             (if (#{:appartment4 :appartment5 :appartment6 :appartment7} app-type)
                                               :appartment4 app-type))})
          (dissoc % :appartment-type-mnemo)))

      (#(if created
          (let [td (tc/today)
                yd (tc/minus td (tc/days 1))
                created (tc/to-time-zone (tco/from-sql-time created)(tc/default-time-zone))
                time-formatter (tf/formatter "HH:mm" (tc/default-time-zone))
                created-str (cond
                             (same-date? created td)(str "Сегодня в " (tf/unparse time-formatter created))
                             (same-date? created yd)(str "Вчера в "(tf/unparse time-formatter created))
                             :else (tf/unparse (tf/formatter "dd.MM.yy в HH:mm" (tc/default-time-zone)) created))]
            (assoc % :created created-str))
          %))))

(defn try-parse-int [s]
  (if (number? s) s
    (try
      (Integer. s)
      (catch NumberFormatException e nil))))

(defn select-resource [entity & [{:keys [query q-convert
                                         fields joins order
                                         exclude-field-preds
                                         post-process page limit]
                                  :or {q-convert identity
                                       post-process identity
                                       exclude-field-preds [(val-nil?)]}}]]
  (let [query (or query {})
        page (dec (or (try-parse-int page) 1))
        raw-limit(env :default-select-limit)
        limit (or limit (if (string? raw-limit) (or (try-parse-int raw-limit) 20) raw-limit))
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
          (#(if fields (apply k/fields % fields) %))
          (#(if joins (joins %) %))
          (#(if order (apply k/order % order) %))
          (k/offset offset)
          (k/limit limit)
          (k/exec)
;;           (#(do (println (k/as-sql %)) (k/exec %)))
          )
      (map post-process)
      (map #(apply exclude-fields % exclude-field-preds))
      vec
      )}))

(defn search-joins [srch]
  (-> srch
   (k/join :districts (= :districts.id :district ))
   (k/join :cities (= :cities.id :pub.city ))
   (k/join :metros (= :metros.id :metro ))
   (k/join :appartment-types (= :appartment-types.id :appartment-type ))
   (k/join :building-types (= :building-types.id :building-type ))
   (k/join :layout-types (= :layout-types.id :toilet))
   ))

(defn search [{:keys [order] :as q} & [page limit]]
  (select-resource :pub {:query q :page page :limit limit
                         :q-convert #(default-converter % conf)
                         :fields [:seoid :created :price
                                  :floor :floors
                                  :address :distance
                                  :total-area :living-area :kitchen-area
                                  :description :imgs :thumb
                                  :deposit :counters :plus-utilities
                                  :plus-electricity :plus-water :plus-gas
                                  :balcony :loggia :bow-window
                                  :furniture :internet
                                  :tv :frige :washer :conditioner
                                  :parking :intercom :security :concierge
                                  :only-russo :kids :pets :addiction
                                  [:districts.name :district]
                                  [:appartment-types.name :appartment-type]
                                  [:building-types.name :building-type]
                                  [:metros.name :metro]]
                         :joins search-joins
                         :order (when order (:mnemo (get os order)))
                         :post-process #(post-process % #{:imgs-cnt :thumb})
                         }))


(defn by-seoid [seoid]
  (->>
   (select-resource :pub {:query {:seoid seoid} :page 1 :limit 1
                          :q-convert #(default-converter % conf)
                          :fields [:seoid :created :price
                                   :floor :floors
                                   :address :distance
                                   :total-area :living-area :kitchen-area
                                   :description :imgs :thumb
                                   :deposit :counters :plus-utilities
                                   :plus-electricity :plus-water :plus-gas
                                   :balcony :loggia :bow-window
                                   :furniture :internet
                                   :tv :frige :washer :conditioner
                                   :parking :intercom :security :concierge
                                   :only-russo :kids :pets :addiction
                                   [:cities.name :city]
                                   [:districts.name :district]
                                   [:appartment-types.mnemo :appartment-type-mnemo]
                                   [:appartment-types.fullname :appartment-type]
                                   [:building-types.name :building-type]
                                   [:layout-types.name :toilet]
                                   [:metros.name :metro]
                                   :lat :lng :person-name]
                          :joins search-joins
                          :post-process #(post-process % #{:imgs-cnt :imgs :description :lat-lng :appartment-type-mnemo})
                          })
  :items
  first))

(defn prepare-phone [{:keys [phone city-code] :as m}]
  (let [phone (read-string phone)
        country-code "+7"]
    {:phone
    (->> phone
         (map (fn [p]
                (let [cnt (count p)]
                  (cond (= 10 cnt)
                        (str country-code "(" (subs p 0 3) ")" (subs p 3 6)"-"(subs p 6 8)"-"(subs p 8))
                        (= 7 cnt)
                        (str country-code "(" city-code ")" (subs p 0 3)"-"(subs p 3 5)"-"(subs p 5))
                        :else p
                        ))))
         vec)}))

(defn phone-by-seoid [seoid]
  (->>
   (select-resource :pub {:query {:seoid seoid} :page 1 :limit 1
                          :q-convert #(default-converter % conf)
                          :fields [:phone
                                   [:cities.default-phone-code :city-code]]
                          :joins #(-> % (k/join :cities (= :cities.id :city)))
                          :post-process prepare-phone
                          })
   :items
   first
   :phone))

(defn agent-by-phone [phone]
  (->>
   (select-resource :agents
                    {:query (if (not= \9 (first phone))
                              (kf/pred-or (kf/pred-= :phone phone)
                                          (kf/pred-= :phone (subs phone 3)))
                              {:phone phone }) :page 1 :limit 1
                          :fields [[:url :last-url] [:updates :seen] [:changed :last-seen]]
                          :post-process #(merge % {:seen (inc (:seen %))
                                                   :last-seen (tf/unparse (tf/formatter "dd.MM.yy в hh:mm") (tco/from-sql-time (:last-seen %)))})
                          })
   :items
   first))

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

(defn empty-query [req]
  {:city (default-city req)
   :order 0})

(defn decode-query [qstr & [req]]
  (let [q (try (edn/read-string qstr)
            (catch Exception err
              (println "Error while parsing query string: " err)(empty-query req)))]
    (merge (empty-query req)(validate-query q))))

(defn try-decode-query [qstr]
  (try (edn/read-string qstr)
    (catch Exception err
      (println "Error while parsing query string: " err)nil)))
