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

(defentity cities)

(defn select-dict [{:keys [dict-key off lim] :as q}]
  (let [dict-key (if (map? q) dict-key q)
        total (-> (select dict-key (aggregate (count :*) :cnt)) first :cnt)
        off (or off 0)
        lim (or lim 500)]
    {:total total
     :offset off
     :limit lim
     :items (select dict-key (offset off) (limit lim))
     }
    ))

(defn key-by-code [dict-key code]
  (-> (select dict-key (fields :id) (where {:code code})) first :id))


(defn by-query [rk q]
  (select-dict :cities)
  )

(defn by-key [rk k]
  (-> (select :cities (where {:id k})) first))

(defn default-data []
  (delete cities)
  (insert cities
          (values [
                   {:code "moscow" :name "москва"}
                   {:code "samara" :name "самара"}
                   {:code "ufa" :name "уфа"}
                   ]
                  )))

(defentity test1)

(defn ins[data]
  (insert :test1 (values data)))
