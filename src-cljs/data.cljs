(ns floor16.datum
  (:require
   [om.core :as om]))

(defn get-cities []
  {:href ""
   :total 80
   :offset 0
   :limit 25
   :items
   [{:id 1 :name "москва" :code "moscow"}
    {:id 2 :name "самара" :code "samara"}
    {:id 3 :name "уфа" :code "ufa"}]})

(def system (atom {:app nil}))

(defn init-data [{:keys [app-state dict-path local?] :as conf}]
  (swap! system merge
         {:app app-state
          :dict-path (or dict-path [:dicts])
          :local? local?}))

(defn app [] (:app @system))

(defn dict-path[] (:dict-path @system))

(defn path-to [k] (conj (dict-path) k))

(defprotocol IIdentity
  (dkey [this])
  (get-by-key [this k]))

(defprotocol IHasName
  (dname [this])
  (name-by-key [this k]))

(defprotocol IHasCode
  (dcode [this])
  (code-by-key [this k])
  (get-by-code [this code])
  (key-by-code [this code]))

(defprotocol IDictionary
  (load-all [this owner k] [this cb])
  (get-all [this]))

(defprotocol IResource
  (load-by-query [this q cursor] [this q cursor korks][this context]))

(defn request-data [res-key cb & [q]]
  ;; TODO
  (if (:local? @system) (cb {})
    (js/setTimeout #(cb (get-cities)) 500)))

(defn- request-cache [dict-key abs-path cb no-cache?]
  (if no-cache?
    (request-data dict-key #(cb (:items %)))
    (let [data (get-in @(app) abs-path)]
      (if (empty? data)
        (request-data dict-key #(cb (get-in (swap! (app) assoc-in abs-path (:items %)) abs-path)))
        (cb (:items data))))))

(defn first-for [kw v items]
  (first (filter #(= (kw %) v) items)))

(defn dict [dict-key & [{:keys [dk dn dc no-cache?]}]]
  (let [abs-path (path-to dict-key)
        dk (or dk :id)
        dn (or dn :name)
        dc (or dc :code)]
    (reify
      IIdentity
      (dkey [this] dk)
      ;; TODO if no-cache?
      (get-by-key [this k] (first-for dk k (get-all this)))
      IHasName
      (dname [this] dn)
      ;; TODO if no-cache?
      (name-by-key [this k] (dn (get-by-key this k)))
      IHasCode
      (dcode [this] dc)
      ;; TODO if no-cache?
      (code-by-key [this k] (dc (get-by-key this k)))
      (get-by-code [this c] (first-for dc c (get-all this)))
      (key-by-code [this c] (dk (get-by-code this c)))
      IDictionary
      (load-all [this cb]
           (request-cache dict-key abs-path cb no-cache?))
      (load-all [this owner k]
           (request-cache dict-key abs-path #(om/set-state! owner k %) no-cache?))
      (get-all [this] (get-in @(app) abs-path))
      )))

(defn kconj [korks k]
  (cond (nil? korks) [k]
        (keyword? korks) [korks k]
        :else (conj korks k)))

(defn res [res-key & [{:keys [dk]}]]
  (let [dk (or dk :id)]
    (reify
      IIdentity
      (dkey [this] dk)
      ;; TODO if no-cache?
      (get-by-key [this k] )
      IResource
      ;; TODO
      (load-by-query [this q cursor] (load-by-query this q cursor nil))
      (load-by-query [this q cursor korks]
                     (println "(kconj korks :loading): "(kconj korks :loading))
                     (om/update! cursor (kconj korks :loading) true)
                     (request-data res-key
                                   #(if korks (om/update! cursor korks %) (om/update! cursor %))
                                   q))
      (load-by-query [this {:keys [query-path data-path] :as cntx}]
                     (swap! (app) assoc-in (kconj data-path :loading) true)
                     (request-data res-key
                                   #(swap! (app) assoc-in data-path %)
                                   (get-in @(app) query-path)))
      )))

(defn current-for [id]
  (let [items (get-in @(app) [:data :items])
        i (first-for :id id items)
        surrogate (when (map? i) (:surrogate i))]
    {:key id
     :surrogate surrogate
     :data {:loading true}}
    ))
