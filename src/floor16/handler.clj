(ns floor16.handler
  (:require [floor16.routes.frontend :refer [front-routes]]
            [floor16.routes.api :refer [api-routes]]
            [floor16.routes.oauth :refer [oauth-routes]]
            [floor16.dal.schema :as schema]
            [environ.core :refer [env]]
            [selmer.parser :as parser]
            [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [compojure.handler :refer [api]]
            ))

(defroutes
  app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when\r
   app is deployed as a servlet on\r
   an app server such as Tomcat\r
   put any initialization code here"
  []
  (when (env :selmer-dev) (parser/cache-off!))
  (when-not (schema/actualized?)(schema/actualize)))

(defn destroy
  "destroy will be called when your application\r
   shuts down, put any clean up code here"
  [])

(defroutes all-routes
  front-routes
  api-routes
  oauth-routes
  app-routes)

(def app (api all-routes))

