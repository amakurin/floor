(ns floor16.routes.frontend
  (:require [compojure.core :refer [defroutes GET]]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [clj-rhino :as js]))

(def app-base-ns "floor16")

(defn app-search-state[req]
  {:text "\"<span></span>"}
  )


(defonce route-conf (atom
                     {:main {:url "/" :template "app-search.html" :app "appsearch" :make-state app-search-state}
                      :grid {:url "/grid" :template "app-search.html" :app "appsearch" :make-state app-search-state}
                      :ad {:url "/ad" :template "app-search.html" :app "appsearch" :make-state app-search-state}
                      :ad-edit {:url "/ad/edit" :template "edit.html"}
                      :ad-create {:url "/ad/create" :template "create.html"}
                      :my-favorites {:url "/my/favor" :template "favor.html"}}
                     ))

(defn do-route [rkw req p]
  (let [{:keys [template app make-state]:as r} (rkw @route-conf)
        template (or template "base.html")
        app (when app (str app-base-ns "." app))
        app-state (when app (or (make-state req) {}))
        app-html (when app "");(react/render app app-state))
        params (if-not app p
                 (-> p
                     (assoc :app app)
                     (assoc :app-state (clojure.string/escape (pr-str app-state) {\" "\\\"" \\ "\\\\"}))
                     (assoc :app-html app-html)))]
    (lt/render template params)))


(defroutes front-routes
  (GET "/" {:as req}
       (do-route :main req {}))
  )
