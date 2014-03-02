(ns friend.workflows.oauth
  (:require
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [cemerick.friend.util :as futils]
   [floor16.http :as http]
   [friend.tokens :as tok]
   [ring.util.request :as req]))

(defonce kw-bad-request-errors
;http://tools.ietf.org/html/rfc6749#page-37
  {:invalid-request {:code "invalid_request" :description "" :uri ""}
   :invalid-client {:code "invalid_client" :description "" :uri ""}
   :invalid-grant {:code "invalid_grant" :description "" :uri ""}
   :unauthorized-client {:code "unauthorized_client" :description "" :uri ""}
   :unsupported-grant-type {:code "unsupported_grant_type" :description "" :uri ""}
   :invalid-scope {:code "invalid_scope" :description "" :uri ""}
   :invalid-token {:code "invalid_token" :description "" :uri ""}
   })

(defn oauth2-token-bad-request
  [kw-error]
  (let [err-conf (kw-error kw-bad-request-errors)
        status 400
        data {:error (:code err-conf)
              :error_description (:description err-conf)
              :error_uri (:uri err-conf)
              }]
  (http/generic-response data status)))

(defn make-bearer-auth [token]
  (workflows/make-auth token
                       {::friend/workflow :oauth2-bearer
                        ::friend/redirect-on-auth? false}))

(defn do-header-auth [token-store authorization realm]
  (when authorization
    (if-let [[_ token-key] (try (-> (re-matches #"\s*Bearer\s+(.+)" authorization))
                             (catch Exception e
                               (.printStackTrace e)
                               (oauth2-token-bad-request :invalid-request)))]
      (let [token (tok/fetch token-store token-key)
            expired? (tok/expired? token)]
        (if (and token (not expired?))
          (make-bearer-auth token)
          (do
            (when expired? (tok/delete token-store token))
            (http/unauthenticated realm))))
      (oauth2-token-bad-request :invalid-request))))

(defn do-logout [token-store auth-map]
  (tok/delete token-store (:identity auth-map))
  (http/generic-response))

(defn do-login [{:keys [realm token-store] :as wf-config}
                {:keys [params] :as request} ]
  (let [{:keys [grant_type username password]} params
        creds {:username username :password password}]
    (cond (and grant_type username password
               (= grant_type "password"))
          (if-let [user-record ((futils/gets :credential-fn wf-config (::friend/auth-config request))
                                (with-meta creds {::friend/workflow :oauth2-bearer}))]
            (make-bearer-auth (tok/new-token token-store user-record))
            (http/unauthenticated realm))
          (not= grant_type "password")
          (oauth2-token-bad-request :unsupported-grant-type)
          :else
          (oauth2-token-bad-request :invalid-request)
          )))

(defn oauth2-bearer
  [& {:keys [realm token-endpoint token-store] :as wf-config
      :or {token-store tok/in-memory-store
           token-endpoint "/oauth/token"
           realm "/"}}]
  (fn [{{:strs [authorization]} :headers, request-method :request-method, :as request}]
    (let [header-result (do-header-auth token-store authorization realm)]
      (if (= token-endpoint (req/path-info request))
        (cond
         (= :delete request-method)
         (if (friend/auth? header-result)
           (do-logout token-store header-result)
           (http/unauthenticated realm))
         (= :post request-method)
         (do
           (when (friend/auth? header-result) (do-logout token-store header-result))
           (do-login {:token-store token-store :realm realm} request))
         :else header-result)
        header-result))))
