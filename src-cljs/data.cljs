(ns floor16.datum
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [put! <! chan]]
   [floor16.xhr :as xhr]
   [om.core :as om]))

(def system (atom {:app nil}))

(defn init-data [{:keys [app-state dict-path local?] :as conf}]
  (swap! system merge
         {:app app-state
          :dict-path (or dict-path [:dicts])
          :local? local?}))

(defn app [] (:app @system))

(defn dict-path[] (:dict-path @system))

(defn path-to [k & [parent]]
  (if-let [[pk pv] parent]
    (conj (dict-path) [pk pv k])
    (conj (dict-path) k)))

(defprotocol IIdentity
  (dkey [this])
  (get-by-key [this k]))

(defprotocol IHasName
  (dname [this])
  (name-by-key [this k]))

(defprotocol IDictionary
  (load-all [this owner k] [this cb]))

(defprotocol IResource
  (load-by-query [this q cursor] [this q cursor korks][this context])
  (load-by-key [this k context]))

(defn api-url [path]
  (str "/api" path))

(defn get-dict-url [dict-key & [[pk pv]]]
    (api-url(str (when pk (str "/"(name pk) "/" pv)) "/"(name dict-key))))

(defn request-dict [dict-key parent cb & [no-cache?]]
  (let [abs-path (path-to dict-key parent)
        url (get-dict-url dict-key parent)]
    (if (and parent (nil? (last parent))) []
      (if no-cache?
        (xhr/cb-request {:method :get :url url} #(cb (:body %)))
        (let [data (get-in @(app) abs-path)]
          (if (empty? data)
            (xhr/cb-request {:method :get :url url}
                            (fn [resp]
                              (cb (get-in (swap! (app) assoc-in abs-path (:body resp)) abs-path))))
            (cb data)))))))

(defn first-for [kw v items]
  (first (filter #(= (kw %) v) items)))

(defn dict-cache [abs-path]
  (get-in @(app) abs-path))

(defn dict [dict-key & [{:keys [dk dn dc no-cache? parent]}]]
  (let [abs-path (path-to dict-key parent)
        ch (chan)
        dk (or dk :id)
        dn (or dn :name)
        dc (or dc :mnemo)]
    (reify
      IIdentity
      (dkey [this] dk)
      (get-by-key [this k] (first-for dk k (dict-cache abs-path)))
      IHasName
      (dname [this] dn)
      (name-by-key [this k] (dn (get-by-key this k )))
      IDictionary
      (load-all [this cb]
                (request-dict dict-key parent #(cb %) no-cache?))
      (load-all [this owner ok]
                (request-dict dict-key parent #(om/set-state! owner ok %) no-cache?))
      )))


(defn get-res-url [res-key & [{:keys [q id]}]]
  (let [{:keys [o-page]} q
        q (dissoc q :o-page)]
    (api-url (str "/" (name res-key)
                  "/" (when id id)
                  (when (or o-page q) "?")
                  (when o-page (str "page=" o-page))
                  (when q (str (when o-page "&") "q=" (js/encodeURIComponent (pr-str q))))))))

(defn request-res [res-key cb & [{:keys [q id] :as context}]]
  (if (:local? @system) (cb {})
    (let [url (get-res-url res-key context)]
      (xhr/cb-request {:method :get :url url} #(cb (:body %))))))

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
                     (request-res res-key
                                   #(if korks (om/update! cursor korks %) (om/update! cursor %))
                                   {:q q}))
      (load-by-query [this {:keys [query query-path data-path] :as cntx}]
                     (swap! (app) assoc-in (kconj data-path :loading) true)
                     (request-res res-key
                                   #(swap! (app) assoc-in data-path %)
                                   {:q (or query (get-in @(app) query-path))}))
      (load-by-key [this k {:keys [current-path] :as cntx}]
                     (swap! (app) assoc-in (concat current-path [:data :loading]) true)
                     (request-res res-key
                                   #(swap! (app) assoc-in (kconj current-path :data) %)
                                   {:id k}))
      )))

(defn current-for [id {:keys [resource-key] :as context}]
  (let [items (get-in @(app) [:data :items])
        i (first-for :id id items)
        surrogate (when (map? i) (:surrogate i))
        r (res resource-key)]
    (load-by-key r id context)
    {:key id
     :surrogate surrogate
     :data {:loading true}}
    ))
