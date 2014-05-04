(ns floor16.appsearch
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [secretary.core :as sec :include-macros true :refer [defroute]]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.ui.generic :as gen]
   [floor16.lang :refer [l]]
   [floor16.navigation :as nav]
   [floor16.xhr :as xhr]
   [floor16.global :as glo]
   [floor16.datum :as dat]
   )
)

(def astate (atom {}))

(declare yama)

(def app-modes
  {:init {:route "/"
          :view-type :list}
   :grid {:route "/ads/"
          :view-type :list
          :data-path [:data]
          :query-path [:query]
          :resource-key :some}
   :ad {:route "/ads/:id"
        :view-type :item-view
        :data-key :id
        }})

(defn simple-filter [query owner]
  (om/component
   (dom/div #js{:className "simple-filter"}
            (om/build gen/select query
                      {:opts {:data-key :city
                              :placeholder (l :city)
                              :dict (dat/dict :cities)
                              :after-update #(nav/url-update {:mode :grid :url-params @query})}})
            (let [url (nav/url-to {:mode :grid :url-params query})]
              (dom/a #js{:href url
                         :onClick (fn [e]
                                    (.preventDefault e)
                                    (nav/goto url)
                                    )} "test-link"))
            )))

(defn extended-filter [data owner opts]
  (om/component
   (dom/div #js{:className ""} "extended-filter")))


(defn ad-item-view [item owner opts]
  (om/component
   (dom/div #js{:className ""} "ad-item-view")))

(defn ad-view [cursor owner]
  (om/component
   (dom/div nil "ad-view")))

(defn app [{:keys [app-mode query data] :as cursor} owner]
  (om/component
   (dom/div nil
            (cond
             (= :grid app-mode)
             (om/build gen/list-view cursor
                       {:opts {:top-filter simple-filter
                               :side-filter extended-filter
                               :header-opts {:sort {:view gen/sort-select}
                                             :views {:view gen/view-select}}
                               :empty-text (l :empty-search)
                               :item-view ad-item-view
                               :res (dat/res :ads)
                               :list-mode :grid
                               :item-view-mode :ad}})
             (= :ad app-mode)
             (om/build ad-view data)
             :else
             (om/build simple-filter query)
             ))))

(defn ^:export yinit[]
  (set! yama js/ymaps)
  (println (.. yama -geolocation -city))
  ;(when-not (get-in @astate [:query :city])
  ;  (swap! astate assoc-in [:query :city] 2))
  )

(defn ^:export main [edn-data]
  (let [data (cljs.reader/read-string edn-data)]
  (println "Main: " data)
    (reset! astate data)
    (dat/init-data {:app-state astate})
    (nav/init-nav {:app-state astate
                   :app-modes app-modes})
    (om/root app astate {:target (.getElementById js/document "app-search")})
    ))

(defn ^:export render [edn-data]
  (set! glo/server-side? true)
  (let [data (cljs.reader/read-string edn-data)]
    (reset! astate data)
    (dat/init-data {:app-state astate :local? true})
    (nav/init-nav {:app-state astate
                   :app-modes app-modes})
    (.renderComponentToString js/React (om/build app (om/to-cursor @astate)))))
