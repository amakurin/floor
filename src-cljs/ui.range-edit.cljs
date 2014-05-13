(ns floor16.ui.range-edit
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
   [clojure.string :as s]
   [goog.fx.Dragger]
   [goog.math.Rect]
   [goog.events :as events]
   [goog.events.EventType]
   [goog.fx.Dragger.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.global :as glo]
   )
  )

(defn ExtendedDragger [el limit]
  (goog/base (js* "this") el nil limit))
(goog/inherits ExtendedDragger goog.fx.Dragger)
(set! (.. ExtendedDragger -prototype -defaultAction) (fn [x y] ))

(defn val-to-pos [value min-btm max-top] (/ (* 100 value) (- max-top min-btm)))

(defn step-round [v step] (*(Math/floor (/ v step)) step))

(defn pos-to-val [pos min-btm max-top step width self-width current]
  (let [pos (+ pos (/ self-width 2))
        raw (/ (* (- max-top min-btm) pos) width)
        stepped (step-round raw step)
        delta (- stepped raw)
        half-step (/ step 2)]
    (if (< (Math/abs delta) half-step) current stepped)))

(defn slider-handle [range-value owner {:keys [bound min-bottom max-top step comm]
                                        :or {step 1} :as opts}]
  (reify
    om/IDidMount
    (did-mount [this]
               (let [node (om/get-node owner)
                     nodeWidth (.-offsetWidth node)
                     parentWidth (.-offsetWidth (.-parentNode node))
                     dragger (ExtendedDragger. node nil (goog.math.Rect. 0 (.-offsetTop node) parentWidth 0))
                     drag-handler
                     (fn [publish] (fn [e]
                                     (put! comm
                                           {:bound bound
                                            :publish publish
                                            :value (pos-to-val (.-left e)
                                                               min-bottom
                                                               max-top
                                                               step
                                                               parentWidth
                                                               nodeWidth
                                                               (bound @range-value))})))
                     ]
                 (om/set-state! owner :dragger dragger)
                 (events/listen dragger goog.fx.Dragger.EventType.DRAG (drag-handler false))
                 (events/listen dragger goog.fx.Dragger.EventType.END (drag-handler true))

                 ))
    om/IWillUnmount
    (will-unmount [this]
                  (when-let [dr (om/get-state owner :dragger)]
                    (.dispose dr)
;;                     (.log js/console this owner)
                    ))
    om/IRender
    (render [this]
            (dom/span #js{:className "slider-handle"
                       :style #js{:left (str (val-to-pos (bound range-value) min-bottom max-top) "%")}
                       :tabIndex 9999
                       :onMouseDown (fn [e] (.focus (.-target e)))
                       :onClick #(.stopPropagation %)
                       }))))

(defn slider-range [range-value owner {:keys [min-bottom max-top]}]
  (om/component
   (dom/div #js{:className "slider-range"
                :style #js{:left (str (val-to-pos (:btm range-value) min-bottom max-top) "%")
                           :width (str (val-to-pos (- (:top range-value) (:btm range-value)) min-bottom max-top) "%")}
                })))

(defn parse-int [v]
  (let [v (s/replace v #"\s" "")]
    (cond (or (not v)(empty? v)) 0
          (re-find #"\D" v) nil
          :else (js/parseInt v))))

(defn actualize [cursor]
  (let [state (.-state cursor)
        path (.-path cursor)]
    (if (empty? path)
      @state
      (get-in @state path))))

(defn range-textbox [range-value owner {:keys [bound comm]}]
  (om/component
   (let [handle-change (fn [e]
                         (if-let [v (parse-int (.. e -target -value))]
                           (put! comm {:bound bound :value v :publish true})
                           (om/refresh! owner)))]
     (dom/input #js {:type "text" :className (name bound)
                     :maxLength 7
                     :value (glo/price-to-str
                             (if (om/get-state owner :editing)
                               (om/get-state owner :value)
                               (bound range-value)))
                     :onKeyDown (fn [e]
                                  (when (= glo/ENTER (glo/key-event->keycode e))
                                    (.. e -target blur)))
                     :onChange (fn [e] (om/set-state! owner :value (s/replace (.. e -target -value) #"\s" "")))
                     :onFocus (fn [_] (om/set-state! owner {:editing true :value (bound @range-value)}))
                     :onBlur (fn [e]
                               (om/set-state! owner :editing false)
                               (handle-change e))}))))

(defn handle-slider-click [{:keys [range-value min-bottom max-top step owner] :as opts}]
  (fn [e]
    (let [node (om/get-node owner "slider")
          pos (- (.-pageX e)
                 (.. node getBoundingClientRect -left))
          raw (/ (* (- max-top min-bottom) pos) (.-offsetWidth node))
          stepped (* (quot raw step) step)
          v (if (< (Math/abs (- stepped raw))(Math/abs (- (+ step stepped) raw ))) stepped (+ step stepped))
          {:keys [btm top]} (actualize range-value)
          bound (if (< (Math/abs (- btm raw))(Math/abs (- top raw))) :btm :top)]
      (put! (om/get-state owner :comm) {:value v :bound bound :publish true}))))

(defn range-editor [range-value owner {:keys [min-bottom max-top step no-text-boxes caption after-update] :as opts}]
  (let [config {:btm (fn [v] (min (max min-bottom v) (:top (actualize range-value))))
                :top (fn [v] (max (min max-top v) (:btm (actualize range-value))))}

        handle-change (fn[{:keys [bound value publish] :as e}]
                        (let [v (or value (bound (actualize range-value)))]
                          (om/transact! range-value [bound] (fn [_] ((bound config) v)))
                          (when (and publish after-update) (after-update))
                          ))
        ]
    (reify
      om/IWillMount
      (will-mount [_]
                  (when-not glo/server-side?
                    (let [comm (chan)]
                      (om/set-state! owner :comm comm)
                      (go (while true
                            (let [e (<! comm)]
                              (handle-change e)))))))
      om/IRender
      (render [_]
              (let [new-opts (assoc opts :comm (om/get-state owner :comm))
                    top-opts (assoc new-opts :bound :top)
                    bottom-opts (assoc new-opts :bound :btm)]
                (dom/div nil
                         (dom/div #js{:className (str "range-edit-wrapper"(when no-text-boxes " no-text-boxes"))}
                                  (when no-text-boxes
                                    (dom/span #js{:className "header clearfix"}
                                              (when caption (dom/span #js{:className "caption"} caption))
                                              (dom/span #js{:className "bounds"} (:btm range-value)" - "(:top range-value))))
                                  (when-not no-text-boxes (om/build range-textbox range-value {:opts bottom-opts}))
                                  (dom/div #js{:ref "slider"
                                               :className "slider"
                                               :onClick (handle-slider-click {:range-value range-value
                                                                              :min-bottom min-bottom
                                                                              :max-top max-top
                                                                              :step step
                                                                              :owner owner})}
                                           (om/build slider-range range-value {:opts opts})
                                           (om/build slider-handle range-value {:opts bottom-opts})
                                           (om/build slider-handle range-value {:opts top-opts})
                                           )
                                  (when-not no-text-boxes (om/build range-textbox range-value {:opts top-opts}))
                                  )))))))






