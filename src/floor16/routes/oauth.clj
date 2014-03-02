(ns floor16.routes.oauth
  (:require [compojure.core :refer [context defroutes GET POST DELETE OPTIONS ANY]]
            [floor16.http :as http]
            [friend.workflows.oauth :as wf-oauth]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [clojure.data.json :as json]
            ))


(def users {"root" {:username "root"
                    :password (creds/hash-bcrypt "admin_password")
                    :roles #{:leaser :householder}}
            "jane" {:username "jane"
                    :password (creds/hash-bcrypt "user_password")
                    :roles #{:leaser}}})

(defn unauthenticated-handler [req]
  (http/unauthenticated))

(defn unauthorized-handler [req]
  (http/generic-response nil 403))

(defn response-identity [req]
  (let [{id :identity, username :username, scope :roles, expires :expires}
        (friend/current-authentication req)]
    (http/generic-response {:token id
                            :username username
                            :fname "Иван"
                            :lname "Полторанин"
                            :roles scope
                            :scope scope
                            :expires expires})))

(defroutes routes
  (GET "/user" req (friend/authorize #{::user} "User"))
  (GET "/admin" req (friend/authorize #{::admin} "Admin"))
  (context "/oauth" []
           (OPTIONS "/" [] (http/options [:options] {:version "2"}))

           (context "/token" []
                    (POST "/" req (friend/authenticated (response-identity req)))
                    (GET "/" req (friend/authenticated (response-identity req)))
                    (OPTIONS "/" [] (http/options [:options :get :post :delete]))
                    (ANY "/" [] (http/method-not-allowed [:options :get :post :delete])))

           (ANY "/" [] (http/method-not-allowed [:options]))))

(defn auth [ring-app]
  (friend/authenticate
   ring-app
   {:credential-fn (partial creds/bcrypt-credential-fn users)
    :workflows [(wf-oauth/oauth2-bearer)]
    :unauthenticated-handler unauthenticated-handler
    :unauthorized-handler unauthorized-handler}))

(def oauth-routes
  (-> routes
      (auth)
      (http/wrap-restful-response)))
