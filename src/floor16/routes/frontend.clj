(ns floor16.routes.frontend
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.response :refer [not-found]]
            [floor16.http :as http]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [floor16.storage :as db]
            [floor16.search :as srch]
            [korma.core :as k]
            [clojure.tools.reader.edn :as edn]
            [clj-rhino :as js]))

(def app-base-ns "floor16")

(defn get-query-dicts [q]
  (let [city (:city q)
        pubsettings {:pubsettings srch/ps}
        ordersettings {:ordersettings srch/os}]
    (merge pubsettings ordersettings
           (when city
             {[:cities city :metros] (k/select :metros
                                               (k/fields :id :mnemo :name :city)
                                               (k/where {:city city}))
              [:cities city :districts] (k/select :districts
                                                  (k/fields :id :mnemo :name :city)
                                                  (k/where {:city city}))}))))

(defn list-specific-state [rconf req]
  (let [{:keys [resource-key]} rconf
        {:keys [page q] :as params} (:params req)
        query (if q (srch/decode-query q req) (srch/empty-query req))
        data  (srch/search query page)
        dicts (get-query-dicts query)]
    {:query query :data data :dicts dicts :settings (srch/get-search-settings)}))

(defn item-specific-state [rconf req]
  (let [{:keys [data-key resource-key]} rconf
        dk (get-in req [:params data-key])
        data (if (and dk resource-key) (srch/by-seoid dk) {})
        query (merge (srch/empty-query req) (when-let [atmnemo (:appartment-type-mnemo data)] {:appartment-type [atmnemo]}))
        dicts (get-query-dicts query)]
    (if (empty? data) {:error 404}
      {:current {:key dk :data (if (:appartment-type-mnemo data) (dissoc data :appartment-type-mnemo) data)}
       :query query :dicts dicts
       :settings (srch/get-search-settings)})))

(defn default-make-state[{:keys [rkw rconf req] :as context}]
  (let [{:keys [mode view-type dicts resource-key]} rconf
        dicts (into {} (map (fn [k] [k (k/select k)]) dicts))
        state {:app-mode mode :dicts dicts}
        ]
    (merge-with merge state
           (when (= :item-view view-type) (item-specific-state rconf req))
           (when (= :list view-type) (list-specific-state rconf req)))))

(defn validate-params [params rc]
  (if-let [{:keys [validate]} rc]
    (validate params)
    true))

(defn do-route [rkw rc req]
  (if (validate-params (:params req) rc)
    (let [{:keys [template app make-state]} rc
          template (or template "base.html")
          app (when app (str app-base-ns "." app))
          contex {:rkw rkw :rconf rc :req req}
          app-state (when app (if make-state (make-state contex) (default-make-state contex)))
          error (:error app-state)
          app-html (when app (react/render app app-state))
          ;app-html ""
          params (if-not app {}
                   (-> {}
                       (assoc :app app)
                       (assoc :app-state (clojure.string/escape (pr-str app-state) {\" "\\\"" \\ "\\\\"}))
                       (assoc :app-html app-html)
                       (#(if error (assoc % :error error) %))))]
        (lt/render template params))
    (http/redirect-to "/")))

(defn item-validate [{:keys [seoid]}]
  (and (not (nil? seoid))
       (= 1 (-> (k/select :pub (k/aggregate (count :*) :cnt) (k/where {:seoid seoid}))
                first :cnt))))

(defn list-validate [{:keys [page q]}]
  (and
   (or (not page) (when-let [p (srch/try-parse-int page)] (> p 0)))
   (or (not q) (srch/try-decode-query q))))

(defn dicts-set-default []
  [:cities :layout-types :building-types])

(def route-conf (atom
                     {:init
                      {:route "/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :grid
                       :view-type :list
                       :dicts (dicts-set-default)
                       :validate list-validate}
                      :ads-search
                      {:route "/ads/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :grid
                       :view-type :list
                       :dicts (dicts-set-default)
                       :resource-key :pub
                       :validate list-validate}
                      :ads-item
                      {:route "/ads/:seoid"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :ad
                       :view-type :item-view
                       :data-key :seoid
                       :dicts (dicts-set-default)
                       :resource-key :pub
                       :validate item-validate
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
  (r-get :ads-item)
)
