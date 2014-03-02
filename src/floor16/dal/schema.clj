(ns floor16.dal.schema
  (:use [lobos.core :only (defcommand migrate)])
  (:require
            [lobos.migration :as lm]))

(def db-spec
  {:subprotocol "mysql"
   :subname "//localhost/floor"
   :user "floor"
   :password "111111"
   :delimiters "`"})

(defcommand pending-migrations []
  (lm/pending-migrations db-spec sname))

(defn actualized?
  "checks if there are no pending migrations"
  [] (empty? (pending-migrations)))

(def actualize migrate)
