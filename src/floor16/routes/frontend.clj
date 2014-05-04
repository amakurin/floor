(ns floor16.routes.frontend
  (:require [compojure.core :refer [defroutes GET]]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [floor16.dal.db :as db]
            [clojure.tools.reader.edn :as edn]
            [clj-rhino :as js]))

(def app-base-ns "floor16")

;(require '[floor16.dal.db :as db])

(defn gen-validate [raw-query]
  (let [allowed-pattern #"[a-zA-Z][a-zA-Z\-\d\?]+"
        allowed-predicate #(or (string? %) (number? %) (vector? %) (keyword? %))]
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

(defn decode-query [qstr & [defaults]]
  (let [q (try (edn/read-string qstr)
            (catch Exception err
              (println "Error while parsing query string: " err){}))]
    (validate-query q)))


(defn list-specific-state [rconf req]
  (let [{:keys [resource-key]} rconf
        {:keys [page q] :as params} (:params req)
        query (if q (decode-query q) {})
        query (if page (assoc query :o-page (edn/read-string page)) query)
        data  (if resource-key (db/by-query resource-key query) {})]
    {:query query :data data}))

(defn item-specific-state [rconf req]
  (let [{:keys [data-key resource-key]} rconf
        dk (get-in req [:params data-key])
        data (if resource-key (db/by-key resource-key dk) {})]
    {:current {:key dk :data data}}))

(defn default-make-state[{:keys [rkw rconf req] :as context}]
  (let [{:keys [mode view-type dicts resource-key]} rconf
        dicts (into {} (map (fn [k] [k (:items (db/select-dict k))]) dicts))
        state {:app-mode mode :dicts dicts}
        ]
    (merge state
           (when (= :item-view view-type) (item-specific-state rconf req))
           (when (= :list view-type) (list-specific-state rconf req)))))

(defn do-route [rkw rc req]
  (let [{:keys [template app make-state]} rc
        template (or template "base.html")
        app (when app (str app-base-ns "." app))
        contex {:rkw rkw :rconf rc :req req}
        app-state (when app (if make-state (make-state contex) (default-make-state contex)))
        app-html (when app (react/render app app-state))
        ;app-html ""
        params (if-not app {}
                 (-> {}
                     (assoc :app app)
                     (assoc :app-state (clojure.string/escape (pr-str app-state) {\" "\\\"" \\ "\\\\"}))
                     (assoc :app-html app-html)))]
    (lt/render template params)))


(defn dicts-set-default []
  [:cities])

(defonce route-conf (atom
                     {:init
                      {:route "/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :init
                       :view-type :list
                       :dicts (dicts-set-default)}
                      :ads-search
                      {:route "/ads/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :grid
                       :view-type :list
                       :dicts (dicts-set-default)
                       :resource-key :some}
                      :ads-item
                      {:route "/ads/:id"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :ad
                       :view-type :item-view
                       :data-key :id
                       :dicts (dicts-set-default)
                       :resource-key :some
                       }
                      }
                     ))
(defn r-get[rkw]
  (let [{:keys [route] :as rc} (rkw @route-conf)]
    (GET route {:as req}
         (println req)
         (do-route rkw rc req))
    ))

(defroutes front-routes
  (r-get :init)
  (r-get :ads-search)
  (r-get :ads-item))
