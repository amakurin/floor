(ns floor16.ui.range-edit
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
   [goog.fx.Dragger]
   [goog.math.Rect]
   [goog.events :as events]
   [goog.events.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   )
  )

(defn ExtendedDragger [el limit]
  (goog/base (js* "this") el nil limit))
(goog/inherits ExtendedDragger goog.fx.Dragger)
(set! (.. ExtendedDragger -prototype -defaultAction) (fn [x y] ))

(defn val-to-pos [value min-btm max-top] (/ (* 100 value) (- max-top min-btm)))

(defn step-round [v step] (*(Math/floor (/ v step)) step))

(defn pos-to-val [pos min-btm max-top step width] (step-round(/ (* (- max-top min-btm) pos) width) step))

(defn slider-handle [range-value owner {:keys [bound min-bottom max-top step comm] :as opts}]
  (reify
    om/IDidMount
    (did-mount [this node]
               (let [parentWidth (.-offsetWidth (.-parentNode node))
                     dragger (ExtendedDragger. node nil (goog.math.Rect. 0 (.-offsetTop node) parentWidth 0))]
                 (events/listen dragger goog.events.EventType.DRAG
                                (fn [e] (put! comm
                                              {:bound bound
                                               :value (pos-to-val (.-left e) min-bottom max-top step parentWidth)})))))
    om/IWillUnmount
    ;;;;; [TODO] DISPOSE DRAGGERS!!!!!
    (will-unmount [this] (.log js/console this owner))
    om/IRender
    (render [this]
            (dom/a #js{:className "slider-handle"
                       :style #js{:left (str (val-to-pos (bound range-value) min-bottom max-top) "%")}
                       ;:onClick (fn [e] (.preventDefault e))
                       }))))

(defn slider-range [range-value owner {:keys [min-bottom max-top]}]
  (om/component
   (dom/div #js{:className "slider-range"
                :style #js{:left (str (val-to-pos (:bottom range-value) min-bottom max-top) "%")
                           :width (str (val-to-pos (- (:top range-value) (:bottom range-value)) min-bottom max-top) "%")}})))

(defn parse-int [v]
  (if (empty? v) 0
    (js/parseInt v)))

(defn range-textbox [range-value owner {:keys [bound comm]}]
  (om/component (dom/input #js {:type "text" :className (name bound)
                                :value (bound range-value)
                                :onChange (fn [e] (put! comm
                                                        {:bound bound
                                                         :value (parse-int (.. e -target -value))}))})))
(defn actualize [cursor]
  (let [state (.-state cursor)
        path (.-path cursor)]
    (if (empty? path)
      @state
      (get-in @state path))))

(defn range-editor [range-value owner {:keys [min-bottom max-top] :as opts}]
  (let [config {:bottom (fn [v] (min (max min-bottom v) (:top (actualize range-value))))
                :top (fn [v] (max (min max-top v) (:bottom (actualize range-value))))}

        handle-change (fn[{:keys [bound value] :as e}]
                        (let [v (if (not(js/isNaN value)) value (bound (actualize range-value)))]
                          (om/transact! range-value [bound] (fn [_] ((bound config) v)))))]
  (reify
    om/IInitState
    (init-state [this]
                (let [comm (chan)]
                  (go (while true
                        (handle-change (<! comm))))
                  {:comm comm}))
    om/IRender
    (render [_]
            (let [new-opts (assoc opts :comm (om/get-state owner [:comm]))
                  top-opts (assoc new-opts :bound :top)
                  bottom-opts (assoc new-opts :bound :bottom)]
              (dom/div nil
                       (dom/div #js{:className "range-edit-wrapper"}
                                (om/build range-textbox range-value {:opts bottom-opts})
                                (dom/div #js{:className "slider"}
                                         (om/build slider-range range-value {:opts opts})
                                         (om/build slider-handle range-value {:opts bottom-opts})
                                         (om/build slider-handle range-value {:opts top-opts})
                                         )
                                (om/build range-textbox range-value {:opts top-opts})
                                )))))))












