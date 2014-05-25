(ns floor16.handler
  (:require [floor16.routes.frontend :refer [front-routes] :as front]
            [floor16.routes.api :refer [api-routes]]
            [floor16.routes.oauth :refer [oauth-routes]]
            [floor16.middleware :as mw]
            [environ.core :refer [env]]
            [selmer.parser :as parser]
            [compojure.core :refer [defroutes ANY]]
            [compojure.route :as route]
            [floor16.storage :as store]
            [compojure.handler :refer [api]]
            ))

(defroutes
  app-routes
  (route/resources "/")
  (ANY "*" {:as req} (front/response-not-found req)))

(defn init
  "init will be called once when\r
  app is deployed as a servlet on\r
  an app server such as Tomcat\r
  put any initialization code here"
  [& [db]]
  (let [db (or db (env :database))
        db (if (string? db) (read-string db) db)]
    (when (env :dev-debug) (parser/cache-off!))
    (store/initialize (or db (env :database)))))

(defn destroy
  "destroy will be called when your application\r
   shuts down, put any clean up code here"
  [])

(defroutes all-routes
  front-routes
  api-routes
  oauth-routes
  app-routes)

(def app (-> all-routes api mw/wrap-check-browser))

