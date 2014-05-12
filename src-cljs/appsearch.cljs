(ns floor16.appsearch
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [secretary.core :as sec :include-macros true :refer [defroute]]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.ui.generic :as gen]
   [floor16.ui.range-edit :as re]
   [floor16.lang :refer [l]]
   [floor16.navigation :as nav]
   [floor16.xhr :as xhr]
   [floor16.global :as glo]
   [floor16.datum :as dat]
   )
)

(def astate (atom {}))

(def app-modes
  {:init {:route "/"
          :view-type :list}
   :grid {:route "/ads/"
          :view-type :list
          :data-path [:data]
          :query-path [:query]
          :resource-key :pub}
   :ad {:route "/ads/:seoid"
        :view-type :item-view
        :data-key :seoid
        }})

(defn after-update [query] #(nav/url-update {:mode :grid :url-params @query}))

(defn simple-filter [query owner]
  (om/component
   (dom/div #js{:className "simple-filter"}
            (let [after-update (after-update query)]
              (dom/div #js{:className "container"}
                       (dom/div #js{:className "location four columns"}
                                (om/build gen/select query
                                          {:opts {:data-key :city
                                                  :placeholder (l :city)
                                                  :dict (dat/dict :cities)
                                                  :after-update (fn [_]
                                                                  (om/transact! query #(-> %
                                                                                           (assoc :metros [])
                                                                                           (assoc :districts [])))
                                                                  (after-update))}})

                                (om/build gen/select query
                                          {:opts {:data-key :metro
                                                  :placeholder (l :metro)
                                                  :className (str "two columns" (when-not (:city query) " disabled"))
                                                  :dict (dat/dict :metros {:parent [:cities (:city query)]})
                                                  :after-update after-update
                                                  :selector gen/checkbtn-list}})
                                (om/build gen/select query
                                          {:opts {:data-key :district
                                                  :placeholder (l :districts)
                                                  :className (str "two columns" (when-not (:city query) " disabled"))
                                                  :dict (dat/dict :districts {:parent [:cities (:city query)]})
                                                  :after-update after-update
                                                  :selector gen/checkbtn-list}})

                                )
                       (dom/div #js{:className "object-types four columns"}
                                (dom/div #js{:className "rooms clearfix"}
                                         (om/build gen/checkbtn (:appartment-type query)
                                                   {:opts {:data-key :room
                                                           :className "room two columns"
                                                           :after-update after-update}})
                                         (om/build gen/checkbtn (:appartment-type query)
                                                   {:opts {:data-key :studio
                                                           :className "two columns"
                                                           :after-update after-update}})
                                         )
                                (dom/div #js{:className "appartments"}
                                         (dom/span #js{:className "flats-label"} (str (l :flat) ":"))
                                         (om/build gen/checkbtn (:appartment-type query)
                                                   {:opts {:data-key :appartment1
                                                           :caption "1"
                                                           :after-update after-update}})
                                         (om/build gen/checkbtn (:appartment-type query)
                                                   {:opts {:data-key :appartment2
                                                           :caption "2"
                                                           :after-update after-update}})
                                         (om/build gen/checkbtn (:appartment-type query)
                                                   {:opts {:data-key :appartment3
                                                           :caption "3"
                                                           :after-update after-update}})
                                         (om/build gen/checkbtn (:appartment-type query)
                                                   {:opts {:data-key :appartment4
                                                           :caption "4+"
                                                           :after-update after-update}})
                                         )
                                )
                       (dom/div #js{:className "price six columns"}
                                (dom/div #js{:className "range"}
                                         (om/build re/range-editor (:price query)
                                                   {:opts {:min-bottom (get-in @astate [:settings :price :btm])
                                                           :max-top (get-in @astate [:settings :price :top])
                                                           :step 500
                                                           :after-update after-update}}))
                                (dom/div #js{:className "options"}
                                         (om/build gen/select query
                                                   {:opts {:data-key :published
                                                           :placeholder (l :published)
                                                           :className "four columns"
                                                           :dict (dat/dict :pubsettings)
                                                           :after-update after-update}})
                                         (om/build gen/checkbtn query
                                                   {:opts {:data-key :with-photo
                                                           :className "with-photo two columns"
                                                           :after-update after-update}}))
                                )
                       (let [url (nav/url-to {:mode :grid :url-params query})]
                         (dom/div #js{:className "two columns"}
                                  (dom/a #js{:href url
                                             :className "search-btn"
                                             :onClick (fn [e]
                                                        (.preventDefault e)
                                                        (nav/goto url)
                                                        )} "Найти жилье")))
                       )))))


(defn box-group [cursor owner {:keys [init-opened
                                      view
                                      caption] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {:opened init-opened})
    om/IRenderState
    (render-state [this {:keys [opened]}]
                  (dom/div #js {:className "box-group"}
                           (dom/span #js{:className "box-group-header clearfix"
                                         :onClick #(om/set-state! owner :opened
                                                                  (not opened))}
                                     (dom/div #js{:className
                                                  (str "box-arrow "
                                                       (if opened
                                                         "arrow-down"
                                                         "arrow-right"))})
                                     (dom/span #js{:className "box-caption"} caption))
                           (when opened
                             (dom/div #js{:className "box-group-content"}
                                      view))))))

(defn extended-filter [query owner opts]
  (om/component
   (let [after-update (after-update query)]
     (dom/div #js{:className "four columns"}
              (dom/div #js{:className "extended-filter"}
                       (dom/span #js{:className "box-header"} (l :additionals))
                       (om/build box-group query
                                 {:opts {:view
                                         (dom/div nil
                                                  (om/build re/range-editor (:total-area query)
                                                            {:opts {:min-bottom (get-in @astate [:settings :total-area :btm])
                                                                    :max-top (get-in @astate [:settings :total-area :top])
                                                                    :step 1
                                                                    :no-text-boxes true
                                                                    :caption "общая, кв.м. "
                                                                    :after-update after-update}})
                                                  (om/build re/range-editor (:living-area query)
                                                            {:opts {:min-bottom (get-in @astate [:settings :living-area :btm])
                                                                    :max-top (get-in @astate [:settings :living-area :top])
                                                                    :step 1
                                                                    :no-text-boxes true
                                                                    :caption "жилая, кв.м. "
                                                                    :after-update after-update}})
                                                  (om/build re/range-editor (:kitchen-area query)
                                                            {:opts {:min-bottom (get-in @astate [:settings :kitchen-area :btm])
                                                                    :max-top (get-in @astate [:settings :kitchen-area :top])
                                                                    :step 1
                                                                    :no-text-boxes true
                                                                    :caption "кухня, кв.м. "
                                                                    :after-update after-update}})
                                                  )
                                         :caption (l :area)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (dom/div nil
                                                  (om/build re/range-editor (:floor query)
                                                            {:opts {:min-bottom (get-in @astate [:settings :floor :btm])
                                                                    :max-top (get-in @astate [:settings :floor :top])
                                                                    :step 1
                                                                    :no-text-boxes true
                                                                    :caption "этаж "
                                                                    :after-update after-update}})
                                                  (om/build re/range-editor (:floors query)
                                                            {:opts {:min-bottom (get-in @astate [:settings :floors :btm])
                                                                    :max-top (get-in @astate [:settings :floors :top])
                                                                    :step 1
                                                                    :no-text-boxes true
                                                                    :caption "этажей в доме "
                                                                    :after-update after-update}}))
                                         :caption (l :floors)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list query
                                                   {:opts {:data-key :building-type
                                                           :className "clearfix"
                                                           :dict (dat/dict :building-types)
                                                           :after-update after-update
                                                           }})
                                         :caption (l :building-type)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list query
                                                   {:opts {:data-key :toilet
                                                           :className "clearfix"
                                                           :dict (dat/dict :layout-types)
                                                           :after-update after-update
                                                           }})
                                         :caption (l :toilet)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list
                                                   query
                                                   {:opts {:className "clearfix"
                                                           :item-class "small"
                                                           :after-update after-update
                                                           :items
                                                           [{:id :balcony     }
                                                            {:id :furniture   }
                                                            {:id :internet    }
                                                            {:id :tv          }
                                                            {:id :frige       }
                                                            {:id :washer      }
                                                            {:id :conditioner }
                                                            {:id :parking     }]}})
                                         :caption (l :facilities)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list
                                                   query
                                                   {:opts {:className "clearfix"
                                                           :item-class "small"
                                                           :after-update after-update
                                                           :items
                                                           [{:id :intercom    }
                                                            {:id :security    }
                                                            {:id :concierge   }]}})
                                         :caption (l :safety)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build re/range-editor (:distance query)
                                                   {:opts {:min-bottom (get-in @astate [:settings :distance :btm])
                                                           :max-top (get-in @astate [:settings :distance :top])
                                                           :step 1
                                                           :no-text-boxes true
                                                           :caption "До метро пешком, мин "
                                                           :after-update after-update}})
                                         :caption (l :distance)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list
                                                   query
                                                   {:opts {:className "clearfix"
                                                           :item-class "small"
                                                           :after-update after-update
                                                           :items
                                                           [{:id :kids    }
                                                            {:id :pets    }]}})
                                         :caption (l :kidsnpets)}})
                       (om/build box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list
                                                   query
                                                   {:opts {:className "clearfix"
                                                           :item-class "small"
                                                           :after-update after-update
                                                           :items
                                                           [{:id :not-only-russo}
                                                            {:id :only-russo}]}})
                                         :caption (l :restrictions)}})
                       )))))


(defn ad-item-view [item owner opts]
  (om/component
   (dom/div #js{:className ""
                :onClick #(nav/url-update {:mode :ad :url-params @item})} (pr-str item))))

(defn ad-view [cursor owner]
  (om/component
   (dom/div #js{:className ""
                :onClick #(nav/go-back)} "ad-view")))

(defn app [{:keys [app-mode query data] :as cursor} owner]
  (om/component
   (dom/div nil
            (cond
             (= :grid app-mode)
             (om/build gen/list-view cursor
                       {:opts {:top-filter simple-filter
                               :side-filter extended-filter
                               :header-opts {:sort {:view gen/sort-select}}
                               :main-container-class "container"
                               :data-container-class "twelve columns"
                               :empty-text (l :empty-search)
                               :item-view ad-item-view
                               :res (dat/res :ads)
                               :list-mode :grid
                               :item-view-mode :ad}})
             (= :ad app-mode)
             (om/build ad-view data)
             :else
             ;; WARN: this is really strange behaviour of react which gets
             ;; Invariant exception cuz of same simple filter in a grid
             (om/build simple-filter query {:react-key "init"})
             ))))

(defn prepare-data [{:keys [query settings] :as data}]
  (if query
    (assoc data :query (merge settings query))
    data))

(defn ^:export main [edn-data]
  (let [data (-> edn-data cljs.reader/read-string prepare-data)]
  (println "Main: " data)
    (reset! astate data)
    (dat/init-data {:app-state astate})
    (nav/init-nav {:app-state astate
                   :app-modes app-modes})
    (om/root app astate {:target (.getElementById js/document "app-search")})
    ))

(defn ^:export render [edn-data]
  (set! glo/server-side? true)
  (let [data (-> edn-data cljs.reader/read-string prepare-data)]
    (reset! astate data)
    (dat/init-data {:app-state astate :local? true})
    (nav/init-nav {:app-state astate
                   :app-modes app-modes})
    (dom/render-to-str (om/build app @astate))))
