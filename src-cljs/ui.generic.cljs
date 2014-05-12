(ns floor16.ui.generic
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
   [floor16.navigation :as nav]
   [floor16.global :as glo]
   ))

(defn active? [owner] (om/get-state owner ::active))

(defn active! [owner v] (om/set-state! owner ::active v))

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
                                      (when-not (glo/in? % (om/get-node owner))
                                        (.stopPropagation %)
                                        (do-close)))]
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

(defn drop-down-menu [cursor owner {:keys [data-key dict close-chan
                                           after-update can-nil nil-caption] :as opts}]
  (reify
    om/IWillMount
    (will-mount [_] (d/load-all dict owner :items))
    om/IWillReceiveProps
    (will-receive-props [this next-props] (d/load-all dict owner :items))
    om/IRenderState
    (render-state [this {:keys [items] :as ss}]
                  ;; todo remove get data on render
                  (let [dk (d/dkey dict)
                        dn (d/dname dict)]
                    (apply dom/ul #js{:className "drop-down-menu"}
                           (map (fn [i] (dom/li #js{:onClick
                                                    (fn [e]
                                                      (om/update! cursor data-key (dk i))
                                                      (when after-update (after-update))
                                                      (put! close-chan :close))}
                                                (dn i)))
                                (if can-nil
                                  (->> items (cons {dk nil dn nil-caption}) vec)
                                  items)))
                    ))))

(defn select [cursor owner
              {:keys [data-key className placeholder
                      selector dict after-update can-nil]:as opts}]
  (om/component
   ;; todo implement display name for refs and colls
   (let [v (data-key cursor)
         display-name (when v (if (coll? v) (str (or placeholder (l :selected))": " (count v))(d/name-by-key dict v)))
         placeholder (or placeholder (l :select-value))
         selector (or selector drop-down-menu)]
     (dom/span #js{:className (str (name data-key) " " className)}
               (dom/span #js{:className "gen-select"
                             :onMouseDown (fn [e] (when-not (active? owner)(active! owner true))
                                            (.preventDefault e))}
                         (if (or (nil? v) (and (coll? v)(empty? v)))
                           (dom/span #js{:className "placeholder"} placeholder)
                           (dom/span #js{:className "value"} display-name))
                         (dom/span #js{:className "btn"}
                                   (dom/div #js{:className (str "drop-arrow " (if (active? owner) "arrow-up" "arrow-down"))}))
                         (when (active? owner)
                           (om/build self-closeable cursor {:opts {:view selector
                                                                   :data-key data-key
                                                                   :do-close #(active! owner false)
                                                                   :dict dict
                                                                   :can-nil can-nil
                                                                   :nil-caption placeholder
                                                                   :after-update after-update}})))))))

(defn bool-get [dk cursor]
  (if (map? cursor)
    (get cursor dk)
    (some #(= dk %) cursor)))

(defn bool-upd [dk v cursor]
  (if (vector? @cursor)
    (if v
      (om/transact! cursor #(-> % (conj dk) distinct vec))
      (om/transact! cursor #(->> % (remove (fn [x] (= x dk))) vec)))
    (if v
      (om/update! cursor dk v)
      (om/transact! cursor #(dissoc % dk)))))

(defn checkbtn [cursor owner
                {:keys [data-key caption className after-update] :as opts}]
  (om/component
   (let [v (bool-get data-key cursor )
         caption (or caption (l data-key))]
     (dom/div #js{:className (str "gen-checkbtn " className)}
              (dom/input #js {:type "checkbox" :checked (boolean v)
                              :onChange (fn [e]
                                          (bool-upd data-key (.. e -target -checked) cursor)
                                          (when after-update (after-update)))})
              (dom/label nil caption)))))

(defn checkbtn-list [cursor owner
                     {:keys [data-key className dict items item-class after-update] :as opts}]
  (reify
    om/IWillMount
    (will-mount [_] (if items
                      (om/set-state! owner :items items)
                      (d/load-all dict owner :items)))
    om/IRenderState
    (render-state [this {:keys [items]}]
                  (apply dom/ul #js{:className (str "gen-checkbtn-list " className)}
                         (map #(dom/li #js{:key (:id %)}
                                       (om/build checkbtn
                                                 (if data-key (data-key cursor) cursor)
                                                 {:opts {:data-key (:id %)
                                                         :caption (:name %)
                                                         :className item-class
                                                         :after-update after-update}}))
                              items)))))

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
  (let [url (when index (nav/url-to {:mode list-mode :url-params (assoc query :o-page index)}))]
    (dom/li #js{:key index
                :className (str "pager-item " (when current? "current ") (when className className))}
            (dom/a #js{:href url
                       :onClick (fn[e] (.preventDefault e)
                                  (when url
                                    (nav/goto url)
                                    (if-let [n (.getElementById js/document "list-view")]
                                      (.scrollTo js/window 0 (.-offsetTop n))
                                      (.scrollTo js/window 0 0))))}
                   (or text index)))))

(defn data-pager [{:keys [query data] :as cursor} owner {:keys [list-mode] :as opts}]
  (om/component
   (let [max-pages 9
         middle (quot max-pages 2)
         total (:total data)
         offset (:offset data)
         limit (:limit data)
         pg-count (.ceil js/Math (/ total limit))
         pg-num (inc (quot offset limit))
         cur-pg (or (:o-page query) pg-num)
         can-right (- pg-count cur-pg)
         to-show (min max-pages pg-count)
         left (- cur-pg (min cur-pg (max (inc middle) (- to-show can-right))))
         right (+ cur-pg (min can-right (max middle (- to-show cur-pg))))
         ]
     (apply dom/ul #js{:className "data-pager"}
             (concat
;              (if (= cur-pg 0) []
                [(om/build page-item query {:opts {:index (when (> cur-pg 1) (dec cur-pg))
                                                   :text (l :data-pager-prev)
                                                   :list-mode list-mode
                                                   :className (str "prev" (when (<= cur-pg 1) " disabled"))}})];)

              (map #(om/build page-item query {:react-key %
                                               :opts {:index % :current? (= % cur-pg) :list-mode list-mode}})
                   (range (inc left) (inc right)))

;              (if (= cur-pg (dec pg-count)) []
                [(om/build page-item query {:opts {:index (when (< cur-pg pg-count )(inc cur-pg))
                                                   :text (l :data-pager-next)
                                                   :list-mode list-mode
                                                   :className (str "next" (when (>= cur-pg pg-count ) " disabled"))}})])))));)


(defn load-progress [data owner opts]
  (om/component
   (dom/div #js{:className "load-progress"} "gen-load-progress")))

(defn list-view [{:keys [query data] :as cursor} owner
                 {:keys [top-filter side-filter
                         data-head header-opts
                         data-empty empty-text
                         item-view kw-id res
                         main-container-class
                         data-container-class
                         list-mode item-view-mode]}]
  (reify
    om/IRenderState
    (render-state [this state]
                  (let [kw-id (or kw-id :id)
                        items (:items data)
                        no-items? (empty? items)]
                    (dom/div #js{:id "list-view"}
                             (when top-filter (om/build top-filter query))
                             (dom/div #js{:className (str "main-container " main-container-class)}
                                      (dom/div #js{:className (str "data-container " data-container-class)}
                                               (when (:loading data) (om/build load-progress data))
                                               (if data-head
                                                 (om/build data-head cursor)
                                                 (om/build data-header cursor {:opts {:header-opts header-opts}}))
                                               (if no-items?
                                                 (if data-empty
                                                   (om/build data-empty {:empty-text empty-text})
                                                   (om/build empty-view {:empty-text empty-text}))
                                                 (apply dom/ul nil
                                                        (map #(dom/li #js{:key (kw-id %)}
                                                                      (om/build item-view %))
                                                             items)))
                                               (when-not no-items? (om/build data-pager cursor {:opts {:list-mode list-mode}})))
                                      (when side-filter (om/build side-filter query)))
                             )))))

