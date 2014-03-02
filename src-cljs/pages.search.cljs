(ns floor16.pages.search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [goog.events :as events]
   [goog.events.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.ui.utils :as uu]
   [floor16.lang :refer [l]]
   [floor16.auth :as auth]
   )
  )

(defn win-mouse-down[e owner do-close]
  (when do-close
    (when-not (uu/in? e (om/get-node owner))
      (do-close))))

(defn self-closeable [cursor owner {:keys [view do-close] :as opts}]
  (reify
	 om/IWillMount
	 (will-mount [_]
					 (let [close-chan (chan)]
						(om/set-state! owner :close-chan close-chan)
						(go (let [e (<! close-chan)] (do-close)))))
    om/IDidMount
    (did-mount [this]
               (let [mouse-handler #(win-mouse-down % owner do-close)]
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

(defn active? [owner] (om/get-state owner :active))

(defn set-active [owner v] (om/set-state! owner :active v))

(defn get-cities []
  {:href ""
   :total 80
   :offset 0
   :limit 25
   :items
   [{:id 1 :name "Москва"}
    {:id 2 :name "Самара"}]})

(defprotocol IResource
  (all [this state])
  (by-key [this state k])
  (name-by-key [this state k])
  (d-key [this])
  (d-name [this])
  (by-query [this state query]))

(defn res-city [abs-path &[dk dn]]
  (let [dk (or dk :id)
        dn (or dn :name)]
    (reify
      IResource
      (all [this state]
           (let [state (if (om/cursor? state) (om/-state state) state)
                 data (get-in @state abs-path)]
             (if (empty? data)
               (get-in (swap! state assoc-in abs-path (:items(get-cities))) abs-path)
               data)))

      (by-key [this state k]
              (first (filter #(= (dk %) k) (all this state))))

      (name-by-key [this state k]
                   (dn (by-key this state k)))
      (d-key [this] dk)

      (d-name [this] dn)

      (by-query [this state query]
                (let [state (if (om/cursor? state) (om/-state state) state)]

                  (swap! state assoc-in (conj abs-path :loading) true)

                  (js/setTimeout #(swap! state assoc-in abs-path (get-cities)) 500)))
      )))

(defn propagade-filter-change [owner dk v]
  (when-let [fc (om/get-state owner ::filter-chan)]
    (put! fc {:d-key dk :value v})))

(defn add-filter-chan [owner & [m]]
  (let [m1 (or m {})]
    (if-let [fc (om/get-state owner ::filter-chan)]
      (assoc-in m1 [:init-state ::filter-chan] fc)
      m)))

(defn set-filter-chan [owner fc]
    (om/set-state! owner ::filter-chan fc))

(defn gen-dd-menu [cursor owner {:keys [data-key resource close-chan] :as opts}]
  (om/component
   ;; todo remove get data on render
   (let [items (all resource cursor)
         dk (d-key resource)
         dn (d-name resource)]
     (apply dom/ul nil
            (map (fn [i] (dom/li #js{:onClick
                                     (fn [e]
                                       (om/update! cursor data-key (dk i))
                                       (propagade-filter-change owner data-key (dk i))
                                       (put! close-chan :close))} (dn i)))
                 items))
     )))

(defn gen-select [cursor owner
                  {:keys [data-key placeholder selector resource]:as opts}]
  (om/component
   ;; todo remove resource call on render
   (let [value (data-key cursor)
         display-name (when value (name-by-key resource cursor value))
         placeholder (or placeholder (l :select-value))
         selector (or selector gen-dd-menu)]
     (dom/span #js{:className "gen-select"
                   :onClick #(set-active owner true)}
               (if (or (nil? value) (and (seq? value)(empty? value)))
                 (dom/span #js{:className "placeholder"} placeholder)
                 (dom/span #js{:className "value"} display-name))
               (dom/span #js{:className "btn"} "...")
               (when (active? owner)
                 (om/build self-closeable cursor
                           (->>{:opts {:view selector
                                       :data-key data-key
                                       :do-close #(set-active owner false)
                                       :resource resource}}
                                      (add-filter-chan owner))))))))

(defn simple-filter [filt owner]
  (om/component
   (dom/div #js{:className "simple-filter"}
            (om/build gen-select filt
                      (->>{:opts {:data-key :city-id
                                  :placeholder (l :city)
                                  :resource (res-city [:common :refs :cities])}}
                                 (add-filter-chan owner))))))

(defn extended-filter [data owner opts]
  (om/component
   (dom/div #js{:className ""} "extended-filter")))

(defn gen-sort-select [data owner opts]
  (om/component
   (dom/div #js{:className ""} "gen-sort-select")))

(defn gen-view-select [data owner opts]
  (om/component
   (dom/div #js{:className ""} "gen-view-select")))

(defn gen-ad-item-view [item owner opts]
  (om/component
   (dom/div #js{:className ""} "gen-ad-item-view")))


(defn gen-empty-view [{:keys [empty-text]} owner]
  (om/component
   (dom/div #js{:className "empty"} empty-text)))

(defn gen-data-header [{filt :filter data :data :as cursor} owner
                       {:keys [header-opts]}]
  (om/component
   (dom/div #js{:className "data-header"}
            (when-let [total (:total data)]
              (dom/div #js{:className "total"}
                       (dom/span #js{:className "count-nums"} total)
                       (dom/span #js{:className "count-words"} (l :ad total))))
            (apply dom/div #js{:className "opts"}
                   (map #(om/build (-> % val :view) filt {:opts (-> % val :opts)}) header-opts))
            )))

(defn gen-page-span [filt owner {:keys [index current? text className]}]
  (dom/li #js{:key index
              :className (str "pager-item " (when current? "current ") (when className className))
              :onClick (fn[_]
                         (om/update! filt [:opts :current-page] index)
                         (propagade-filter-change owner [:opts :current-page] index))}
          (or text (inc index))))

(defn gen-data-pager [{filt :filter data :data :as cursor} owner opts]
  (om/component
   (let [max-pages 9
         middle (quot max-pages 2)
         total (:total data)
         offset (:offset data)
         limit (:limit data)
         pg-count (.ceil js/Math (/ total limit))
         pg-num (quot offset limit)
         cur-pg (or (-> filt :opts :current-page) pg-num)
         can-right (- (dec pg-count) cur-pg)
         to-show (min max-pages pg-count)
         left (- cur-pg (min cur-pg (max middle (- (dec to-show) can-right))))
         right (+ cur-pg (min can-right (max middle (- (dec to-show) cur-pg))))
         ]
     (apply dom/ul #js{:className "data-pager"}
             (concat
              (if (= cur-pg 0) []
                [(om/build gen-page-span filt
                           (->> {:opts {:index (dec cur-pg) :text (l :data-pager-prev) :className "prev"}}
                                (add-filter-chan owner)))])

              (map #(om/build gen-page-span filt (->> {:opts {:index % :current? (= % cur-pg)}}
                                                      (add-filter-chan owner)))
                   (range left (inc right)))

              (if (= cur-pg (dec pg-count)) []
                [(om/build gen-page-span filt
                           (->> {:opts {:index (inc cur-pg) :text (l :data-pager-next) :className "next"}}
                                (add-filter-chan owner)))])
              )))))

(defn gen-load-progress [data owner opts]
  (om/component
   (dom/div #js{:className "load-progress"} "gen-load-progress")))


(defn gen-list-view [{filt :filter data :data :as cursor} owner
                     {:keys [top-filter side-filter
                             data-header header-opts
                             empty-view empty-text
                             item-view kw-id res]}]
  (reify
    om/IWillMount
    (will-mount [_]
                (let [filter-chan (chan)]
                  (set-filter-chan owner filter-chan)
                  (go
                   (while true
                     (let [{:keys [d-key value]} (<! filter-chan)]
                       (by-query res cursor filt)
                       (println d-key value))))))
    om/IRenderState
    (render-state [this state]
                  (let [kw-id (or kw-id :id)
                        items (:items data)
                        no-items? (empty? items)]
                    (dom/div nil
                             (when top-filter (om/build top-filter filt (add-filter-chan owner)))
                             (dom/div #js{:className "data-container"}
                                      (when (:loading data) (om/build gen-load-progress data))
                                      (if data-header
                                        (om/build data-header cursor (add-filter-chan owner))
                                        (om/build gen-data-header cursor (->> {:opts {:header-opts header-opts}}
                                                                              (add-filter-chan owner))))
                                      (if no-items?
                                        (if empty-view
                                          (om/build empty-view (om/graft {:empty-text empty-text} cursor))
                                          (om/build gen-empty-view (om/graft {:empty-text empty-text} cursor)))
                                        (apply dom/ul nil
                                               (map #(dom/li #js{:key (kw-id %)}
                                                             (om/build item-view %))
                                                    items)))
                                      (when-not no-items? (om/build gen-data-pager cursor (add-filter-chan owner))))
                             (when side-filter (om/build side-filter filt (add-filter-chan owner))))))))


(defn page [cursor owner {:keys [page-id page-path] :as opts}]
  (om/component
   (dom/div #js{:id "sec-home"}
            (dom/div #js{:id "spot" :className "design-layer"})
            (dom/div #js{:id "icons" :className "design-layer"})
            (dom/div #js{:id "cells" :className "design-layer"})
            (dom/div #js{:className "container"}
                     (when (auth/guest?)
                       (dom/div #js{:className "advert"}
                                (dom/div #js{:id "slogan" :className "eight columns"}
                                         (dom/h1 nil
                                                 (dom/img #js {:src "/img/slogan.png"
                                                               :alt "Найди жилье сам! Прямая аренда от собственников!"}))
                                         (dom/p nil
                                                "Ежедневные обновления базы жилых объектов"
                                                (dom/br nil)
                                                "SMS и E-mail рассылка подходящих вариантов"
                                                (dom/br nil)
                                                "Гибкая система тарификации"))
                                (dom/div #js{:id "scheme" :className "offset-by-one six columns"}
                                         (dom/img #js {:src "/img/scheme.png"
                                                       :alt "Удобный поиск. Ежедневные обновления. Автоподбор вариантов. SMS и E-mail рассылка. Онлайн консультации."}
                                                  ))
                                (dom/div #js{:className "clear"})))
                     (let [pg (get-in cursor page-path)]
                       (if (empty? (:filter pg))
                         (om/build simple-filter (:filter pg))
                         (om/build gen-list-view pg
                                   {:opts {:top-filter simple-filter
                                           :side-filter extended-filter
                                           :header-opts {:sort {:view gen-sort-select}
                                                         :views {:view gen-view-select}}
                                           :empty-text (l :empty-search)
                                           :item-view gen-ad-item-view
                                           :res (res-city (conj page-path :data))}})))
                     ))))

