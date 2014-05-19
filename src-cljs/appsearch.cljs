(ns floor16.appsearch
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [secretary.core :as sec :include-macros true :refer [defroute]]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :as s]
   [floor16.ui.generic :as gen]
   [floor16.maps :as maps]
   [floor16.photo :as pht]
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
          :view-type :list
          :data-path [:data]
          :query-path [:query]
          :resource-key :pub}
   :grid {:route "/ads/"
          :view-type :list
          :data-path [:data]
          :query-path [:query]
          :resource-key :pub}
   :ad {:route "/ads/:seoid"
        :view-type :item-view
        :resource-key :pub
        :data-key :seoid
        :current-path [:current]
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
                                                        (nav/goto url true)
                                                        (.preventDefault e)
                                                        )} "Найти жилье")))
                       )))))



(defn extended-filter [query owner opts]
  (om/component
   (let [after-update (after-update query)]
     (dom/div #js{:className "four columns offset-by-one"}
              (dom/div #js{:className "extended-filter"}
                       (dom/span #js{:className "box-header"} (l :additionals))
                       (om/build gen/box-group query
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
                       (om/build gen/box-group query
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
                       (om/build gen/box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list query
                                                   {:opts {:data-key :building-type
                                                           :className "clearfix"
                                                           :dict (dat/dict :building-types)
                                                           :after-update after-update
                                                           }})
                                         :caption (l :building-type)}})
                       (om/build gen/box-group query
                                 {:opts {:view
                                         (om/build gen/checkbtn-list query
                                                   {:opts {:data-key :toilet
                                                           :className "clearfix"
                                                           :dict (dat/dict :layout-types)
                                                           :after-update after-update
                                                           }})
                                         :caption (l :toilet)}})
                       (om/build gen/box-group query
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
                       (om/build gen/box-group query
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
                       (om/build gen/box-group query
                                 {:opts {:view
                                         (om/build re/range-editor (:distance query)
                                                   {:opts {:min-bottom (get-in @astate [:settings :distance :btm])
                                                           :max-top (get-in @astate [:settings :distance :top])
                                                           :step 1
                                                           :no-text-boxes true
                                                           :caption "До метро пешком, мин "
                                                           :after-update after-update}})
                                         :caption (l :distance)}})
                       (om/build gen/box-group query
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
                       (om/build gen/box-group query
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

(defn phone-button [{:keys [seoid phone] :as cursor} owner {:keys [className title]
                                                            :or {title (l :phone-button)}:as opts}]
  (om/component
   (let [response-handler (fn [x] (om/update! cursor :phone x))]
     (if phone
       (dom/span #js{:className (str "show-phone opened "(when (> (count phone) 1) "cnt ") className)} (s/join " " phone))
       (dom/span #js{:className (str "show-phone " className)
                     :onClick #(dat/api-get :private response-handler seoid)} title)
       ))))

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
        title (compose-title item :area)
        link-handler (fn [e]
                       (when (= 0 (.-button e))
                         (nav/goto url)
                         (if-let [n (.getElementById js/document "ad")]
                                      (.scrollTo js/window 0 (.-offsetTop n))
                                      (.scrollTo js/window 0 0))
                         (.preventDefault e)))]
    (dom/div #js{:className "ad-item eleven columns"}
             (dom/span #js{:className "pub-date"} (get-time-text item))
             (dom/a #js{:className "thumb two columns"
                        :href url :title (compose-title item :photo)
                        :onClick link-handler}
                    (when thumb (dom/img #js{:src thumb :alt (compose-str item)}))
                    (when (and imgs-cnt (< 0 imgs-cnt))
                      (dom/span #js{:className "photo-count"} imgs-cnt)))
             (dom/div #js{:className "descr six columns"}
                      (dom/a #js{:className "address six columns"
                                 :href url :title title
                                 :onClick link-handler}
                             (if address address (l :no-address)))
                      (compose-digest item))
             (dom/div #js{:className "cond three columns"}
                      (render-price item "")
                      (dom/div #js{:className "price-details"} (when price
                                                                 (str (when deposit "+ депозит ")
                                                                      (when plus-utilities "+ ком.платежи"))))
                      (om/build phone-button item {:opts {:className "three columns"}}))
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

(defn ad-view [{:keys [query current] :as cursor} owner]
  (let [{:keys [appartment-type
                price deposit plus-utilities
                address lat
                imgs
                description created
                loading] :as data} (:data current)]
    (om/component
     (if loading
       (dom/div nil loading)
       (dom/div #js{:className "ad-view" :id "ad"}
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
                                  (dom/div #js{:className (str "info" (when (empty? imgs) " auto"))}

                                           (dom/span #js{:className "address"}
                                                     (if address address (l :no-address)))

                                           (render-metdis data "")

                                           (render-props data)

                                           (om/build phone-button data)
                                           ))
                         (when (seq imgs)
                           (dom/div #js{:className "ten columns"}
                                    (om/build pht/photo-viewer imgs {:opts {:img-alt (compose-str data)}})))
                         (when description
                           (dom/div #js{:className (str "description " (if (seq imgs) "sixteen" "ten") " columns")}
                                    (dom/h3 #js{:className ""} "описание")
                                    (dom/p nil description)))
                         (when lat
                           (dom/div #js{:className "location sixteen columns"}
                                    (dom/h3 #js{:className ""} "расположение")
                                    (om/build maps/map-viewer data {:opts {:className "row sixteen columns alpha omega"}})))
                         ))))))



(defn app [{:keys [app-mode query data current] :as cursor} owner]
  (om/component
   (dom/div nil
            (cond
             (= :ad app-mode)
             (om/build ad-view cursor)
             :else
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
                               :loading-text (l :loading-search)
                               :item-view ad-item-view
                               :res (dat/res :ads)
                               :list-mode :grid}})
;;              :else
             ;; WARN: this is really strange behaviour of react which gets
             ;; Invariant exception cuz of same simple filter in a grid
;;              (om/build simple-filter query {:react-key "init"})
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


