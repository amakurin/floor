(ns floor16.routes.frontend
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.response :refer [not-found status]]
            [floor16.http :as http]
            [floor16.views.layout :as lt]
            [floor16.views.reactor :as react]
            [floor16.storage :as db]
            [floor16.search :as srch]
            [clojure.string :as s]
            [korma.core :as k]
            [clojure.tools.reader.edn :as edn]
            [clj-rhino :as js]))

(def app-base-ns "floor16")

(defn get-query-dicts [q]
  (let [city (:city q)
        pubsettings {:pubsettings srch/ps}
        ordersettings {:ordersettings srch/os}]
    (merge pubsettings ordersettings
           (when city
             {[:cities city :metros] (k/select :metros
                                               (k/fields :id :mnemo :name :city)
                                               (k/where {:city city}))
              [:cities city :districts] (k/select :districts
                                                  (k/fields :id :mnemo :name :city)
                                                  (k/where {:city city}))}))))

(defn list-specific-state [rconf req]
  (let [{:keys [resource-key]} rconf
        {:keys [page q] :as params} (:params req)
        query (if q (srch/decode-query q req) (srch/empty-query req))
        data  (srch/search query page)
        dicts (get-query-dicts query)]
    {:query query :data data :dicts dicts :settings (srch/get-search-settings req)}))

(defn item-specific-state [rconf req]
  (let [{:keys [data-key resource-key]} rconf
        dk (get-in req [:params data-key])
        data (if (and dk resource-key) (srch/by-seoid dk) {})
        query (merge (srch/empty-query req) (when-let [atmnemo (:appartment-type-mnemo data)] {:appartment-type [atmnemo]}))
        dicts (get-query-dicts query)]
    (if (empty? data) {:error 404}
      {:current {:key dk :data (if (:appartment-type-mnemo data) (dissoc data :appartment-type-mnemo) data)}
       :query query :dicts dicts
       :settings (srch/get-search-settings req)})))

(defn static-state [rconf req]
  (let [{:keys [resource-key]} rconf
        query (srch/empty-query req)
        dicts (get-query-dicts query)]
    {:query query :dicts dicts :data {}
     :settings (srch/get-search-settings req)}))

(defn default-make-state[{:keys [rkw rconf req] :as context}]
  (let [{:keys [mode view-type dicts resource-key spec-state]} rconf
        dicts (into {} (map (fn [k] [k (k/select k)]) dicts))
        state {:app-mode mode :dicts dicts}
        ]
    (merge-with merge state
           (when spec-state (spec-state rconf req)))))

(defn validate-params [params rc]
  (if-let [validate (:validate rc)]
    (validate params)
    true))

(def default-title "Робот ЭТАЖ16")

(defn get-title [state rc]
  (if-let [{:keys [create-seo-title]} rc]
    (create-seo-title state)
    default-title))

(defn get-description [state rc]
  (if-let [{:keys [create-seo-description]} rc]
    (create-seo-description state)
    default-title))

(defn convert-district [district]
  (s/replace district #"(ий|ый)$" "ом"))

(defn convert-city [city]
  (-> city
      (s/replace #"а$" "е")
      (s/replace #"(ч)$" #(str (last %1) "и"))
      (s/replace #"(ь)$" "и")
      (s/replace #"[крдгзтлв]$" #(str (last %1) "е"))))

(defn list-title [{:keys [query dicts] :as state}]
  (let [{:keys [city]} query
        cities (:cities dicts)
        city (->> cities (filter #(= (:id %) city)) first :name)]
    (str "Снять квартиру в " (convert-city city) " без посредников")))

(defn list-description [{:keys [query dicts] :as state}]
  (let [{:keys [city]} query
        cities (:cities dicts)
        city (->> cities (filter #(= (:id %) city)) first :name)]
    (str "Умный поиск жилья в "(convert-city city) ". Предложения от собственников без посредников и комиссий.")))

(defn convert-app-type [appartment-type]
  (-> appartment-type
      (s/replace #"комнатная" "комнатную")
      (s/replace #"я$" "ю")
      (s/replace #"а$" "у")
      ))

(defn item-title [{:keys [current] :as state}]
  (let [{:keys [appartment-type city price total-area]} (:data current)]
    (str "Снять " (convert-app-type appartment-type) " в " (convert-city city)
         (if price
           (str " без посредников за " price "р")
           (str " " total-area "квм от собственника")))))

(defn item-description [{:keys [current] :as state}]
  (let [{:keys [appartment-type
                city
                price
                total-area floor floors
                district]} (:data current)]
    (str "Сдается " appartment-type " в " (convert-city city)
         (when district (str ", в " (convert-district district) " р-не"))
         (when total-area (str ", площадь " (int total-area) "квм"))
         (when floor (str " на " floor " этаже" (when floors (str " " floors " этажного дома"))))
         (when price (str " за " price "р."))
         " от собственника, без посредников и комиссий")))

(defn agents-title [state]
  "База агентов недвижимости в Самаре")

(defn agents-description [state]
  "Полная база агентов по аренде недвижимости в Самаре. Проверка по номеру телефона.")

(defn do-route [rkw rc req]
  (if (validate-params (:params req) rc)
    (let [{:keys [template app make-state mode]} rc
          template (or template "base.html")
          app (when app (str app-base-ns "." app))
          contex {:rkw rkw :rconf rc :req req}
          app-state (when app (if make-state (make-state contex) (default-make-state contex)))
          app-state (assoc app-state :gmap-url "https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false&language=ru")
          error (:error app-state)
          app-html (when app (react/render app app-state))
          ;app-html ""
          params (if-not app {}
                   (-> {}
                       (assoc :app app)
                       (assoc :app-state (clojure.string/escape (pr-str app-state) {\" "\\\"" \\ "\\\\"}))
                       (assoc :app-html app-html)
                       (assoc :title (get-title app-state rc))
                       (assoc :description (get-description app-state rc))
                       (assoc :loadmaps (= mode :ad))
                       (#(if error (assoc % :error error) %))))]
        (lt/render template params))
    (http/redirect-to "/")))

(defn item-validate [{:keys [seoid]}]
  (and (not (nil? seoid))
       (= 1 (-> (k/select :pub (k/aggregate (count :*) :cnt) (k/where {:seoid seoid}))
                first :cnt))))

(defn list-validate [{:keys [page q]}]
  (and
   (or (not page) (when-let [p (srch/try-parse-int page)] (> p 0)))
   (or (not q) (srch/try-decode-query q))))

(defn dicts-set-default []
  [:cities :layout-types :building-types])

(def route-conf (atom
                     {:init
                      {:route "/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :grid
                       :view-type :list
                       :dicts (dicts-set-default)
                       :spec-state list-specific-state
                       :validate list-validate
                       :create-seo-title list-title
                       :create-seo-description list-description}
                      :ads-search
                      {:route "/ads/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :grid
                       :view-type :list
                       :dicts (dicts-set-default)
                       :spec-state list-specific-state
                       :resource-key :pub
                       :validate list-validate
                       :create-seo-title list-title
                       :create-seo-description list-description}
                      :ads-item
                      {:route "/ads/:seoid"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :ad
                       :view-type :item-view
                       :spec-state item-specific-state
                       :data-key :seoid
                       :dicts (dicts-set-default)
                       :resource-key :pub
                       :validate item-validate
                       :create-seo-title item-title
                       :create-seo-description item-description
                       }
                      :agents
                      {:route "/agents/"
                       :template "app-search.html"
                       :app "appsearch"
                       :mode :agents
                       :view-type :static
                       :spec-state static-state
                       :dicts (dicts-set-default)
                       :resource-key :agents
                       :create-seo-title agents-title
                       :create-seo-description agents-description}
                      :agreement
                      {:route "/agreement/"
                       :template "agreement.html"
                       :app "appsearch"
                       :mode :none
                       :view-type :static
                       :spec-state static-state
                       :dicts (dicts-set-default)
                       :create-seo-title (fn [_]"Пользовательское соглашение")
                       :create-seo-description (fn [_]"Пользовательское соглашение")}
                      }
                     ))

(defn r-get[rkw]
  (let [{:keys [route] :as rc} (rkw @route-conf)]
    (GET route {:as req}
         (if (:old-ie? req)
           (.render (lt/render "oldbrowser.html") req)
           (do-route rkw rc req)))
    ))

(defroutes front-routes
  (r-get :init)
  (r-get :ads-search)
  (r-get :ads-item)
  (r-get :agents)
  (r-get :agreement))

(defn response-not-found [req]
  (let [rc {:route ""
            :template "app-search.html"
            :app "appsearch"
            :mode :not-found
            :view-type :static
            :spec-state static-state
            :dicts (dicts-set-default)
            :create-seo-title (fn [_] "Страница не найдена")
            :create-seo-description (fn [_] "Страница не найдена, возможно она была удалена, либо вы неправильно ввели ссылку.")}
        ]
    (if (:old-ie? req)
      (.render (lt/render "oldbrowser.html") req)
      (status (.render (do-route :not-found rc req) req) 404))))
