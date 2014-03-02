(ns clauth.store.mysql
  (:require [clauth.store :refer [Store]]
            [floor16.dal.db :as db]))

(def entity-keys
  "Map clauth namespace keywords to db entities"
  {;:tokens db/auth-tokens
   ;:auth-codes db/auth-codes
   :clients db/client-apps
   :users db/users})

(defrecord MysqlStore [ek]
  Store
  (fetch [this kv] (db/fetch (entity-keys ek) kv))
  (revoke! [this t] (db/del (entity-keys ek) ))
  (store! [this key-param item] )
  (entries [this] )
  (reset-store! [this] ))

(defn create-mysql-store
  "Create a mysql store"
  ([ek] (MysqlStore. ek)))
