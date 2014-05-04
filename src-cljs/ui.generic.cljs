(ns floor16.ui.generic
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [goog.dom :as gd]
   [goog.events :as events]
   [goog.events.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.lang :refer [l]]
   [floor16.auth :as auth]
   [floor16.datum :as d]
   [floor16.navigation :as nav]
   ))

(defn active? [owner] (om/get-state owner ::active))

(defn active! [owner v] (om/set-state! owner ::active v))

(defn el-matcher [el]
  (fn [other] (identical? other el)))

(defn in? [e el]
  (let [target (.-target e)]
    (or (identical? target el)
        (not (nil? (gd/getAncestor target (el-matcher el)))))))

(defn self-closeable [cursor owner {:keys [view do-close] :as opts}]
  (reify
    om/IWillMount
    (will-mount [_]
                (let [close-chan (chan)]
                  (om/set-state! owner :close-chan close-chan)
                  (go (let [e (<! close-chan)] (do-close)))))
    om/IDidMount
    (did-mount [this]
               (let [mouse-handler #(when do-close
                                      (when-not (in? % (om/get-node owner))(do-close)))]
                 (events/listen js/window goog.events.EventType.MOUSEDOWN mouse-handler)
                 (om/set-state! owner [::window-listener] mouse-handler)))
    om/IWillUnmount
    (will-unmount [this]
                  (when-let [mouse-handler (om/get-state owner [::window-listener])]
                    (events/unlisten js/window goog.events.EventType.MOUSEDOWN mouse-handler)))
    om/IRenderState
    (render-state [this {:keys [close-chan]}]
                  (om/build view cursor {:init-state (om/get-state owner)
                                         :opts (-> opts (dissoc [:view :do-close])
                                                   (assoc :close-chan close-chan))}))))

(defn drop-down-menu [cursor owner {:keys [data-key dict close-chan after-update] :as opts}]
  (reify
    om/IDidMount
    (did-mount [_] (om/set-state! owner :items (d/get-all dict)))

    om/IRenderState
    (render-state [this {:keys [items]}]
                  ;; todo remove get data on render
                  (let [dk (d/dkey dict)
                        dn (d/dname dict)]
                    (apply dom/ul nil
                           (map (fn [i] (dom/li #js{:onClick
                                                    (fn [e]
                                                      (om/update! cursor data-key (dk i))
                                                      (after-update)
                                                      (put! close-chan :close))} (dn i)))
                                items))
                    ))))

(defn select [cursor owner
                  {:keys [data-key placeholder selector dict after-update]:as opts}]
  (om/component
   ;; todo implement display name for refs and colls
   (let [v (data-key cursor)
         display-name (when v (d/name-by-key dict v))
         placeholder (or placeholder (l :select-value))
         selector (or selector drop-down-menu)]
     (dom/span #js{:className "gen-select"
                   :onClick #(active! owner true)}
               (if-not v
                 (dom/span #js{:className "placeholder"} placeholder)
                 (dom/span #js{:className "value"} display-name))
               (dom/span #js{:className "btn"} "...")
               (when (active? owner)
                 (om/build self-closeable cursor {:opts {:view selector
                                                         :data-key data-key
                                                         :do-close #(active! owner false)
                                                         :dict dict
                                                         :after-update after-update}}))))))

(defn sort-select [data owner opts]
  (om/component
   (dom/div #js{:className ""} "gen-sort-select")))

(defn view-select [data owner opts]
  (om/component
   (dom/div #js{:className ""} "gen-view-select")))

(defn empty-view [{:keys [empty-text]} owner]
  (om/component
   (dom/div #js{:className "empty"} empty-text)))

(defn data-header [{:keys [query data] :as cursor} owner
                   {:keys [header-opts]}]
  (om/component
   (dom/div #js{:className "data-header"}
            (when-let [total (:total data)]
              (dom/div #js{:className "total"}
                       (dom/span #js{:className "count-nums"} total)
                       (dom/span #js{:className "count-words"} (l :ad total))))
            (apply dom/div #js{:className "opts"}
                   (map #(om/build (-> % val :view) query {:opts (-> % val :opts)}) header-opts))
            )))

(defn page-item [query owner {:keys [index current? text className list-mode]}]
  (let [url (nav/url-to {:mode list-mode :url-params (assoc query :o-page index)})]
    (dom/li #js{:key index
                :className (str "pager-item " (when current? "current ") (when className className))}
            (dom/a #js{:href url
                       :onClick (fn[e] (.preventDefault e)(nav/goto url))}
                   (or text (inc index))))))

(defn data-pager [{:keys [query data] :as cursor} owner {:keys [list-mode] :as opts}]
  (om/component
   (let [max-pages 9
         middle (quot max-pages 2)
         total (:total data)
         offset (:offset data)
         limit (:limit data)
         pg-count (.ceil js/Math (/ total limit))
         pg-num (quot offset limit)
         cur-pg (or (:o-page query) pg-num)
         can-right (- (dec pg-count) cur-pg)
         to-show (min max-pages pg-count)
         left (- cur-pg (min cur-pg (max middle (- (dec to-show) can-right))))
         right (+ cur-pg (min can-right (max middle (- (dec to-show) cur-pg))))
         ]
     (apply dom/ul #js{:className "data-pager"}
             (concat
              (if (= cur-pg 0) []
                [(om/build page-item query {:opts {:index (dec cur-pg)
                                                   :text (l :data-pager-prev)
                                                   :list-mode list-mode
                                                   :className "prev"}})])

              (map #(om/build page-item query {:opts {:index % :current? (= % cur-pg) :list-mode list-mode}})
                   (range left (inc right)))

              (if (= cur-pg (dec pg-count)) []
                [(om/build page-item query {:opts {:index (inc cur-pg)
                                                   :text (l :data-pager-next)
                                                   :list-mode list-mode
                                                   :className "next"}})]))))))

(defn load-progress [data owner opts]
  (om/component
   (dom/div #js{:className "load-progress"} "gen-load-progress")))


(defn list-view [{:keys [query data] :as cursor} owner
                 {:keys [top-filter side-filter
                         data-head header-opts
                         data-empty empty-text
                         item-view kw-id res
                         list-mode item-view-mode]}]
  (reify
    om/IRenderState
    (render-state [this state]
                  (let [kw-id (or kw-id :id)
                        items (:items data)
                        no-items? (empty? items)]
                    (dom/div nil
                             (when top-filter (om/build top-filter query))
                             (dom/div #js{:className "data-container"}
                                      (when (:loading data) (om/build load-progress data))
                                      (if data-head
                                        (om/build data-head cursor)
                                        (om/build data-header cursor {:opts {:header-opts header-opts}}))
                                      (if no-items?
                                        (if data-empty
                                          (om/build data-empty (om/graft {:empty-text empty-text} cursor))
                                          (om/build empty-view (om/graft {:empty-text empty-text} cursor)))
                                        (apply dom/ul nil
                                               (map #(dom/li #js{:key (kw-id %)}
                                                             (om/build item-view %))
                                                    items)))
                                      (when-not no-items? (om/build data-pager cursor {:opts {:list-mode list-mode}})))
                             (when side-filter (om/build side-filter query))
                             )))))
