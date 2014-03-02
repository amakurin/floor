(ns floor16.routes.api
  (:require [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [floor16.dal.db :as db]))

;cities
;city/:id
;area-types
;area-type/:id
;city/:id/area-types
;city/:id/areas type=metro,district,street
;area/:id
;appartment-types
;appartment-type/:id
;appartments type=room,1room,2room,3room,4room city-id area-id ...
;appartment/:id
;client-types
;client-type/:id
;clients type=realtor,householder,leaseholder
;owner/:oid/clients
;client/:id
;users
;owner/:oid/users
;user/:id

(defn api-url ([url] (format "/api/v1/%s" url))
              ([url param regex] [(api-url url), param regex]))

(def cities (atom [{:id 0
                    :code "moskva"
                    :caption "Москва"
                    :area-types [:metro :district :street]
                    }
                   {:id 1
                    :code "samara"
                    :caption "Самара"
                    :area-types [:district :street]}]))

;(defresource res-cities
;  :available-media-types ["text/html"]
;  :handle-ok "<html>res-cities</html>")

(defresource res-city [id]
  :available-media-types ["text/html" "application/json"]
  :handle-ok (fn [_] (format "<html>res-city id: %s</html>" ((db/get-user "test-id") :first_name))))

(defroutes api-routes
  (ANY (api-url "cities") []
       (->
        (resource
           :available-media-types ["application/json" "application/edn"]
           :handle-ok (fn [ctx] @cities));(get-in ctx [:representation :media-type])
          (wrap-restful-response)
        )
  )
  (ANY [(api-url "city/:id") :id #"[0-9]+"] [id] (res-city id)))
