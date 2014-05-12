(ns floor16.routes.api
  (:use korma.core)
  (:require [compojure.core :refer [defroutes context
                                    ANY GET POST PUT
                                    DELETE OPTIONS] :as com]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [environ.core :refer [env]]
            [floor16.http :as http]
            [floor16.search :as srch]
            ))

(defmacro res-dict [entity & [condition fields]]
  `(try
     (http/generic-response
      (-> (select* ~entity)
          (#(apply fields % (or ~fields [:id :name :mnemo])))
          (#(if ~condition (where % ~condition) %))
          (select)))
     (catch Exception e# (http/gen-error))))

(defroutes api-routes
  (->>
   (context (env :api-url) []
            (OPTIONS "/" []
                     (http/options [:options] {:version "0.1.0"}))
            (ANY "/" []
                 (http/method-not-allowed [:options]))
            (context "/cities" []
                     (GET "/" []
                          (res-dict :cities))
                     (GET "/:id" [id]
                          (res-dict :cities {:id id}))
                     (GET "/:id/districts" [id]
                          (res-dict :districts {:city id}))
                     (GET "/:id/metros" [id]
                          (res-dict :metros {:city id}))
                     (OPTIONS "/" []
                              (http/options [:options :get]))
                     (ANY "/" []
                          (http/method-not-allowed [:options :get])))
            (context "/districts" []
                     (GET "/:id" [id]
                          (res-dict :districts {:city id}))
                     (OPTIONS "/" []
                              (http/options [:options :get]))
                     (ANY "/" []
                          (http/method-not-allowed [:options])))
            (context "/metros" []
                     (GET "/:id" [id]
                          (res-dict :metros {:id id}))
                     (OPTIONS "/" []
                              (http/options [:options :get]))
                     (ANY "/" []
                          (http/method-not-allowed [:options])))
            (context "/pub" []
                     (GET "/:id" [id]
                          (res-dict :metros {:id id}))
                     (GET "/" req
                          (let [{:keys [page q] :as params} (:params req)
                                query (if q (srch/decode-query q req) (srch/empty-query req))
                                data  (srch/search query page)]
                            (http/generic-response data)))
                     (OPTIONS "/" []
                              (http/options [:options :get]))
                     (ANY "/" []
                          (http/method-not-allowed [:options]))))
   (wrap-restful-response)
   ))
