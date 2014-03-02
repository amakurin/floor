(ns floor16.appsearch
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
;   [goog.events :as events]
;   [goog.events.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
;   [floor16.ui.utils :as uu]
;   [floor16.lang :refer [l]]
;   [floor16.auth :as auth]
   ))

(defn test-app [app owner]
  (om/component
   (dom/div nil
            (dom/span #js{:onClick #(om/update! app :text "Omg! I Am Clicked!")} (:text app))
            (dom/span #js{:onClick #(om/update! app :text "Omg! I Am Clicked Tooo!")} "I Am Just Static Text :0)")
            (dom/input #js{:type "text"
                           :value (:text app)
                           :onChange #(om/update! app :text (.. % -target -value)) })
            )))

(def as (atom {}))

(defn ^:export main [edn-data]
  (println (cljs.reader/read-string "{:text \"\\\"sfsf\"}") {:text "sdfsd"} edn-data)
  (let [data (cljs.reader/read-string edn-data)]
  (reset! as data)
  (println (om/to-cursor as))
  (om/root test-app as {:target (.getElementById js/document "app-search")})
  ))

(defn ^:export render [edn-data]
  (let [data (cljs.reader/read-string edn-data)]
    (.renderComponentToString js/React (om/build test-app (om/to-cursor data)))))
