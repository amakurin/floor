(ns floor16.maps
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

(defn map-viewer [{:keys [lat lng] :as cursor} owner
                  {:keys [map-zoom  className marker-text] :or {map-zoom 15} :as opts}]
  (reify
    om/IInitState
    (init-state [this] {:radius 40 :has-pano true :build-map false})
    om/IDidMount
    (did-mount [this]
               (let [service (google.maps.StreetViewService.)
                     lat-lng (google.maps.LatLng. lat lng)
                     max-radius 2000
                     svs-handler (fn svs-handler [data status]
                                   (let [r (om/get-state owner :radius)]
                                     (cond
                                      (= status google.maps.StreetViewStatus.OK)
                                      (om/set-state! owner {:has-pano true :build-map true :pano-lat-lng (.. data -location -latLng)})
                                      (<= r max-radius)
                                      (let [new-rad (+ r (if (<= r (/ max-radius 2)) 50 100))]
                                        (om/set-state! owner :radius new-rad)
                                        (.getPanoramaByLocation service lat-lng new-rad svs-handler))
                                      :else (om/set-state! owner {:has-pano false :build-map true}))))
                     ]
                 (.getPanoramaByLocation service lat-lng (om/get-state owner :radius) svs-handler)
                 ))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
                (when-let [build-map (om/get-state owner :build-map)]
                  (let [lat-lng (google.maps.LatLng. lat lng)
                        gmap (google.maps.Map. (.getElementById js/document "map") #js{:center lat-lng :zoom map-zoom :panControl false})
                        marker (google.maps.Marker. #js{:position lat-lng :map gmap :title marker-text})
                        ]
                    (when-let [pano-lat-lng (om/get-state owner :pano-lat-lng)]
                      (.setStreetView gmap
                                      (google.maps.StreetViewPanorama.
                                       (.getElementById js/document "pano")
                                       #js{:position pano-lat-lng  :addressControl false :pov #js{:heading 50 :pitch 0}}
                                       ))))
                  (om/set-state! owner :build-map false)))
    om/IRenderState
    (render-state [this {:keys [has-pano]}]
                  (dom/div #js{:className (str "map-viewer " className)}
                           (dom/div #js{:id "map" :className (if has-pano "eight columns alpha" "sixteen columns alpha omega no-pano")})
                           (when has-pano (dom/div #js{:id "pano" :className "eight columns omega"}))
                           ))))
