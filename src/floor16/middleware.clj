(ns floor16.middleware
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.response :refer [not-found status]]
            [floor16.http :as http]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [floor16.storage :as db]
            [floor16.search :as srch]
            [clojure.string :as s]
            [korma.core :as k]
            [clojure.tools.reader.edn :as edn]
            [clj-rhino :as js]))

(defn old-ie? [ua & [comparer to]]
  (let [ver (second (re-find #"MSIE (\d)" ua))
        opera (re-find #"(?iux)opera" ua)
        comparer (or comparer <)
        to (or to 9)]
    (when (and (not opera) ver)
      (comparer (Integer. ver) 9))))

(defn wrap-check-browser
  [app]
  (fn [req]
    (app (assoc req :old-ie? (old-ie? (get-in req [:headers "user-agent"]))))))
