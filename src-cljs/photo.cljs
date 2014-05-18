(ns floor16.photo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [goog.events :as events]
   [goog.events.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.lang :refer [l]]
   [floor16.auth :as auth]
   [floor16.datum :as d]
   [floor16.global :as glo]
   ))

(defn photo-viewer [imgs owner {:keys [className img-alt] :as opts}]
  (reify
    om/IInitState
    (init-state [this] {:current 0})
    om/IRenderState
    (render-state [this {:keys [current] :or {current 0}}]
                  (let [total (count imgs)]
                  (dom/div #js{:className (str "photo-viewer " className)}
                           (dom/img #js{:src (get imgs current)
                                        :alt (str img-alt " фото: " (inc current) " из " total)
                                        :onClick #(om/set-state! owner :current (mod (inc current) total))})
                           (when (> total 1)
                             (dom/span #js{:className "arrow prev"
                                         :onClick #(om/set-state! owner :current (mod (dec current) total))}))
                           (when (> total 1)
                             (dom/span #js{:className "arrow next"
                                         :onClick #(om/set-state! owner :current (mod (inc current) total))}))
                           (dom/span #js{:className "photo-number"} (str (inc current)"/"total))
                           )))))
