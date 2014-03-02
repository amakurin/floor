(ns floor16.dal.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [floor16.dal.schema :as schema]))

(def db-spec
  {:subprotocol "mysql"
   :subname "//localhost/floor"
   :user "floor"
   :password "111111"})

(defdb db schema/db-spec)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Generic Data Access Layer
(defn create [entity obj]
        (insert entity
                (values obj)))

(defn store [entity obj]
        (update entity
                (set-fields obj)))

(defn fetch [entity kv]
       (first (select entity
                      (where {:id kv})
                      (limit 1))))

(defn del [entity kv]
     (delete entity
             (where {:id kv})))

(defn del-all [entity]
         (delete entity))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; users

(defentity users)

(defn create-user [user]
  (insert users
          (values user)))

(defn update-user [id first-name last-name email]
  (update users
          (set-fields {:first_name first-name
                       :last_name last-name
                       :email email})
          (where {:id id})))
(defn ge [] users)

(defn get-user [id]
  (first (select (ge)
                 (where {:id id})
                 (limit 1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; client-apps
(defentity client-apps)


