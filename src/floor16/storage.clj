(ns floor16.storage
  (:use
   korma.core
   [korma.db :only (defdb)])
  (:require
   [environ.core :refer [env]]
   ))

(defn initialize [conf]
  (when-not (resolve 'db)
    (defdb db conf)))

;(initialize (env :database))
;(defdb db (env :database))

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
  (select :cities))

