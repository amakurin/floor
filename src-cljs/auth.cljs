(ns floor16.auth
  (:require [floor16.xhr :as xh]
            [floor16.cookies :as cook]
            [om.core :as om :include-macros true]))

(def sys-data (atom {}))

(defn app [] (:app @sys-data))

(defn api-url [] (:api-url @sys-data))

(defn get-user-param [kw] (get-in @(app) [:user kw]))

(defn token [] (get-user-param :token))

(defn auth-coockie [meth & [t]]
  (if token (meth :session-id t) (meth :session-id)))

(defn handle-autologin-response []
  (fn [response]
    (println "Autologin response:" response)
    (if (= 200 (:status response))
      (swap! (app) assoc :user (assoc (:body response) :autologin true))
      (auth-coockie cook/cremove))))

(defn auth-sys [{app :app url :api-url}]
  (swap! sys-data #(-> %
                       (assoc :app app)
                       (assoc :api-url url)))
  (when-let [t (auth-coockie cook/cget)]
    (xh/do-request
     (->{:method :get
         :url (api-url)}
                 (xh/bearer-authorization t))
     (handle-autologin-response))))

(defn handle-login-response [& [cb err-cb]]
  (fn [response]
    (println "Login response:" response)
    (if (= 200 (:status response))
      (do
        (swap! (app) assoc :user (:body response))
        (auth-coockie cook/cset (token))
        (when cb (cb)))
      (when err-cb (err-cb)))))

(defn handle-logout-response []
  (fn [response]
    (println "Logout response:" response)
    (auth-coockie cook/cremove)
    (swap! (app) dissoc :user)
    ))

;;;API
(defn get-user [] (:user @(app)))
(defn do-login [creds] (do-login creds nil nil))
(defn do-login [creds cb] (do-login creds cb nil))
(defn do-login [creds cb err-cb]
              (xh/do-request
               (->{:method :post
                   :url (api-url)
                   :body {:grant_type "password"
                          :username (:username creds)
                          :password (:password creds)}}
                           (xh/accept :edn)
                           (xh/content-type :url-encode))
               (handle-login-response cb err-cb)))
(defn do-logout []
               (when-let [t (token)]
                 (xh/do-request
                  (->{:method :delete
                      :url (api-url)}
                              (xh/bearer-authorization t))
                  (handle-logout-response))))
(defn guest? [] (nil? (token)))

(defn autologin? [] (get-user-param :autologin))
