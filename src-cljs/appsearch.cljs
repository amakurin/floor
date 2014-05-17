(ns floor16.appsearch
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [secretary.core :as sec :include-macros true :refer [defroute]]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :as s]
   [floor16.ui.generic :as gen]
   [floor16.ui.range-edit :as re]
   [floor16.lang :refer [l] :as lng]
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
                                                           :className "three columns"
                                                           :dict (dat/dict :pubsettings)
                                                           :after-update after-update}})
                                         (om/build gen/checkbtn query
                                                   {:opts {:data-key :with-photo
                                                           :className "with-photo three columns"
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
     (dom/div #js{:className "four columns offset-by-one"}
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
(defn compose-title[{:keys [appartment-type total-area imgs-cnt] :as item} area-or-photo-kw]
  (str "Перейти к объявлению: сдается " appartment-type
       (when (and (= :area area-or-photo-kw) total-area) (str " площадью " total-area))
       (when (and (= :photo area-or-photo-kw) imgs-cnt (< 0 imgs-cnt)) (str " " imgs-cnt " фото")) "..."))

(defn compose-str[{:keys [appartment-type total-area floor floors] :as item}]
  (str "Сдается " appartment-type
       (when total-area (str " " total-area "кв.м. "))
       (when floor (str "на " floor "-м этаже "))
       (when floors (str " " floors (lng/ru-floor-ending floors) " " (if floor "этажного дома" "этажном доме")))))


(defn render-metdis [{:keys [metro distance district] :as item} className]
  (dom/span #js{:className className}
            (when metro (dom/strong #js{:className "metro"} (dom/i nil) metro))
            (when metro  (dom/span #js{:className "distance"
                                       :title "пешком"} (str "(∼" distance " мин.) ")))
            (when district (dom/strong #js{:className "district"} (dom/i nil) district))))

(defn compose-digest[{:keys [appartment-type total-area
                             floor floors
                             building-type
                             metro distance district] :as item}]
  (dom/span #js{:className "digest"}
            (dom/span #js{:className "first-line six columns"}
                      (dom/span nil "Сдается ")
                      (dom/strong nil appartment-type)
                      (when total-area
                        (dom/strong nil
                                    " "
                                    total-area
                                    "м"
                                    (dom/sup nil 2)
                                    " ")))
            (dom/span #js{:className "second-line six columns"}
                      (when floor (dom/span nil
                                            "на "
                                            (dom/strong nil floor)
                                            "-м этаже "))
                      (when (and (not floor) (or floors building-type)) (dom/span nil "в "))
                      (when floors (dom/span nil
                                             " "
                                             (dom/strong nil floors)
                                             (str (lng/ru-floor-ending floors) " ")
                                             (if floor "этажного " "этажном ")))
                      (when building-type (dom/strong nil (s/replace building-type #"ый$" (if floor "ого " "ом "))))
                      (when (or floors building-type) (if floor "дома " "доме ")))
            (render-metdis item "third-line six columns")))

(defn get-add-fields [{:keys [balcony loggia bow-window] :as item} has]
  (let [src [:balcony :loggia :bow-window
                     :furniture :internet
                     :tv :frige :washer :conditioner
                     :parking :intercom :security :concierge
                     :kids :pets :addiction]]
    (->> src
         (filter #(= has (% item)))
         vec)))

(defn render-has [item has & [no-label]]
  (let [fields (get-add-fields item has)]
    (if (seq fields)
      (apply
       dom/span #js{:className (if has "has" "hasnt")}
       (concat
        (when-not no-label [(dom/span #js{:className "lbl" :key "lbl"} (l (if has :has :hasnt)))])
        (map #(dom/i #js{:className (str "icon "(name %)) :title (if (and (not has)
                                                                             (or (= % :kids)
                                                                                 (= % :pets)))
                                                                      (l (keyword (str "no-"(name %))))
                                                                      (l %)) :key (name %)})fields)))
      "")))

(defn get-time-text [{:keys [created] :as item}]
  created)

(defn render-price [{:keys [price deposit plus-utilities] :as item} & [className]]
  (dom/span #js{:className (str (when-not price "no ") "price " className)}
            (when price (dom/span #js{:className "val"} (glo/price-to-str price)))
            (when price (dom/span #js{:className "cur"} (l :rub)))
            (when-not price (l :no-price))))

(defn ad-item-view [{:keys [seoid
                            floor floors
                            price deposit plus-utilities
                            address
                            description imgs-cnt
                            thumb] :as item} owner opts]
  (let [url (nav/url-to {:mode :ad :url-params item})
        title (compose-title item :area)]
    (dom/div #js{:className "ad-item eleven columns"}
             (dom/span #js{:className "pub-date"} (get-time-text item))
             (dom/a #js{:className "thumb two columns"
                        :href url :title (compose-title item :photo)}
                    (when thumb (dom/img #js{:src thumb :alt (compose-str item)}))
                    (when (and imgs-cnt (< 0 imgs-cnt))
                      (dom/span #js{:className "photo-count"} imgs-cnt)))
             (dom/div #js{:className "descr six columns"}
                      (dom/a #js{:className "address six columns"
                                 :href url :title title}
                             (if address address (l :no-address)))
                      (compose-digest item))
             (dom/div #js{:className "cond three columns"}
                      (render-price item "")
                      (dom/div #js{:className "price-details"} (when price
                                                                 (str (when deposit "+ депозит ")
                                                                      (when plus-utilities "+ ком.платежи"))))
                      (dom/span #js{:className "show-phone three columns"} "показать номер"))
             (dom/span #js{:className "additionals offset-by-two nine columns"}
                       (dom/span #js{:className "icons"}
                                 (render-has item true)
                                 (render-has item false)))
             )))


(defn compose-areas [{:keys [total-area living-area kitchen-area]}]
  (cond (and total-area living-area kitchen-area) (str total-area " / " living-area" / " kitchen-area)
        (and living-area kitchen-area) (str "x / " living-area" / " kitchen-area)
        (and total-area kitchen-area) (str total-area " / x / " kitchen-area)
        (and total-area living-area) (str total-area " / " living-area" / x")
        total-area (str total-area)))

(defn compose-floors [{:keys [floor floors]}]
  (cond (and floor floors) (str floor " / " floors)
        floor (str floor)
        floors (str "x / " floors)))

(defn render-props [{:keys [building-type toilet person-name] :as cursor} & [className]]
  (let [areas (compose-areas cursor)
        floors (compose-floors cursor)
        has (get-add-fields cursor true)
        hasnt (get-add-fields cursor false)]
    (dom/table #js{:className (str "props " className)}
               (dom/tbody nil
                          (when areas
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :area))
                                    (dom/td nil (str areas " м")
                                            (dom/sup nil "2"))))
                          (when floors
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :floor))
                                    (dom/td nil floors)))
                          (when building-type
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :building-type))
                                    (dom/td nil building-type)))
                          (when toilet
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :toilet))
                                    (dom/td nil toilet)))
                          (when (or (seq has)(seq hasnt))
                            (dom/tr #js{:className "break"} (dom/td nil)))
                          (when (seq has)
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :has))
                                    (dom/td nil (render-has cursor true true))))
                          (when (seq hasnt)
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :hasnt))
                                    (dom/td nil (render-has cursor false true))))
                          (when person-name
                            (dom/tr #js{:className "break"} (dom/td nil)))
                          (when person-name
                            (dom/tr nil
                                    (dom/td #js{:className "key"} (l :person-name))
                                    (dom/td nil person-name)))
                          ))))

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

(defn ad-view [{:keys [query current] :as cursor} owner]
  (let [{:keys [appartment-type
                price deposit plus-utilities
                address lat
                imgs
                description created] :as data} (:data current)]
    (om/component
     (dom/div #js{:className "ad-view"}
              (om/build simple-filter query)
              (dom/div #js{:className "container"}
                       (dom/h2 #js{:className "ad-header row sixteen columns"}
                               (dom/span #js{:className "twelve columns alpha"}
                                         (dom/span #js{:className "rent-word"} "сдается ")
                                         (dom/span #js{:className "app-type"} (str " " appartment-type))
                                         (dom/span #js{:className "pub"} (str "опубликовано " created)))
                               (dom/span #js{:className "price-wrap four columns omega"}
                                         (render-price data "")
                                         (dom/span #js{:className "price-details"}
                                                   (when price
                                                     (str (when deposit "+ депозит ")
                                                          (when plus-utilities "+ ком.платежи"))))))
                       (dom/div #js{:className "six columns"}
                                (dom/div #js{:className "info"}

                                         (dom/span #js{:className "address"}
                                                   (if address address (l :no-address)))

                                         (render-metdis data "")

                                         (render-props data)

                                         (dom/span #js{:className "show-phone"} "показать номер")
                                         ))
                       (when (seq imgs)
                         (dom/div #js{:className "ten columns"}
                                  (om/build photo-viewer imgs {:opts {:img-alt (compose-str data)}})))
                       (when description
                         (dom/div #js{:className (str "description " (if (seq imgs) "sixteen" "ten") " columns")}
                                  (dom/h3 #js{:className ""} "описание")
                                  (dom/p nil description)))
                       (when lat
                         (dom/div #js{:className "location sixteen columns"}
                                  (dom/h3 #js{:className ""} "расположение")
                                  (om/build map-viewer data {:opts {:className "row sixteen columns alpha omega"}})))
                       )))))



(defn app [{:keys [app-mode query data current] :as cursor} owner]
  (om/component
   (dom/div nil
            (cond
             (= :grid app-mode)
             (om/build gen/list-view cursor
                       {:opts {:top-filter simple-filter
                               :side-filter extended-filter
                               :header-opts {:sort {:view
                                                    (dom/span nil
                                                    (dom/span #js{:className "order-label four columns"} "Сортировать по")
                                                    (om/build gen/select query
                                                              {:opts {:data-key :order
                                                                      :placeholder (l :order)
                                                                      :className "three columns"
                                                                      :dict (dat/dict :ordersettings)
                                                                      :after-update (after-update query)}}))
                                                    }}
                               :main-container-class "container"
                               :data-container-class "eleven columns"
                               :data-header-class "clearfix"
                               :data-header-total-kword :ad
                               :data-header-total-class "four columns"
                               :data-header-opts-class "seven columns"
                               :empty-text (l :empty-search)
                               :item-view ad-item-view
                               :res (dat/res :ads)
                               :list-mode :grid}})
             (= :ad app-mode)
             (om/build ad-view cursor)
             :else
             ;; WARN: this is really strange behaviour of react which gets
             ;; Invariant exception cuz of same simple filter in a grid
             (om/build simple-filter query {:react-key "init"})
             ))))

(defn prepare-data [{:keys [app-mode query settings current] :as data}]
  (if query
    (assoc data :query (merge settings query (when (and current (-> current :data :appartment-type)) )))
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


