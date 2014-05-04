(ns floor16.navigation
  (:require [om.core :as om :include-macros true]
            [secretary.core :as sec :include-macros true :refer [defroute]]
            [goog.history.Html5History.TokenTransformer]
            [goog.events :as events]
            [goog.events.EventType]
            [floor16.global :as glo]
            [floor16.datum :as dat]
            )
  (:import [goog.history Html5History]
           [goog.history EventType]))

(when-not glo/server-side? (enable-console-print!))

(declare history)

(def system (atom {:server-state? true}))

(defn- app [] (:app @system))

(defn decode-query [qstr]
  (try (cljs.reader/read-string qstr)
    (catch js/Object err
      (println "Error while parsing query string: " err){})))

(defn encode-query [q]
  (pr-str q))

(defn compose-data-key [r]
  (fn [{:keys [mode-conf url-params result-params] :as context}]
    (if (= (:view-type mode-conf) :item-view)
      (let [dk (:data-key mode-conf)
            dv (dk url-params)]
        (assert dv (str "Data-key (" dk ") value is missed in url-params: " url-params))
        (r (assoc-in context [:result-params dk] dv)))
      (r context))))

(defn compose-raw-query [r]
  (fn [{:keys [mode-conf url-params result-params] :as context}]
    (if (= (:view-type mode-conf) :list)
      (r (if (empty? url-params) context (assoc-in context [:result-params :query-params :q] (encode-query url-params))))
      (r context))))

(defn compose-page-number [r]
  (fn [{:keys [url-update? o-page mode-conf url-params result-params] :as context}]
    (if (= (:view-type mode-conf) :list)
      (let [{op :o-page} url-params
            page (if url-update? 0(or o-page op))]
        (r (if page
             (-> context
                 (assoc :url-params (dissoc url-params :o-page))
                 (assoc-in [:result-params :query-params :page] page))
             context)))
      (r context))))

(defn render-url [{:keys [mode-conf result-params] :as context}]
    (sec/render-route (:route mode-conf) result-params))

(defn url-to [{:keys [mode] :as context}]
  (let [mconf (mode (:app-modes @system))]
    (assert mconf (str "App-mode conf was not found: " mode))
    ((:url-composer @system) (merge context {:mode-conf mconf :result-params {}}))))

(defn- handle-history-event [token]
  ;warning! handle first event cuz it from server
  (if (:server-state? @system)
    (swap! system dissoc :server-state?)
    (do
      ;(println "event! history token: " token)
      (sec/dispatch! (str "/" token))
      )))

(defn- retrieve-token [pathPrefix location]
  (str (subs (.-pathname location) (count pathPrefix))  (.-search location)))

(defn- create-url [token pathPrefix location]
  (str pathPrefix token))

(defn hTokenTransformer []
  (goog/base (js* "this")))
(goog/inherits hTokenTransformer goog.history.Html5History.TokenTransformer)
(set! (.. hTokenTransformer -prototype -retrieveToken) retrieve-token)
(set! (.. hTokenTransformer -prototype -createUrl) create-url)


(defn- init-history []
  (set! history (Html5History. nil (hTokenTransformer.)))
  (.setUseFragment history false)
  (.setEnabled history true)
  (events/listen history EventType/NAVIGATE #(handle-history-event (.-token %)))
)

(defn handle-query-params [h]
  (fn [{:keys [mode mode-conf params result-state] :as context}]
    (let [{:keys [view-type query-path]} mode-conf
          query-params (:query-params params)
          qstr (get query-params "q")
          page (get query-params "page")]
      (if (= :list view-type)
        (h (update-in context (cons :result-state query-path)
                      merge
                      (when qstr (decode-query qstr))
                      ;;todo try\catch on read
                      (when page {:o-page (cljs.reader/read-string page)})))
        (h context)))))

(defn handle-data-key [h]
  (fn [{:keys [mode mode-conf params] :as context}]
    (if (= (:view-type mode-conf) :item-view)
      (let [dk (:data-key mode-conf)
            dv (dk params)]
        (assert dv (str "Data-key (" dk ") value is missed in params: " params))
        (h (assoc-in context [:result-state :current] (dat/current-for dv))))
      (h context))))

(defn handle-route [{:keys [mode mode-conf result-state] :as context}]
  (swap! (app) merge (assoc result-state :app-mode mode))
  (let [{:keys [data-path query-path resource-key data-updater]} mode-conf]
    (when data-updater (data-updater {:resource-key resource-key
                                      :query-path query-path
                                      :data-path data-path}))))

(defn do-route [{:keys [mode mode-conf] :as context}]
    (assert mode-conf (str "App-mode conf was not found: " mode))
    ((:route-handler @system) (assoc context :result-state {})))

(defn default-data-load [{:keys [resource-key query-path data-path]}]
  (let [res (dat/res resource-key)]
    (dat/load-by-query res {:query-path query-path :data-path data-path})))

(defn- init-routes [modes]
  (doseq [[mode conf] modes]
    (let [{:keys [route view-type] :as mode-conf}
          (merge {:data-path [:data]
                  :query-path [:query]
                  :data-key :id
                  :data-updater (when (= :list (:view-type conf)) default-data-load)} conf)]
      ; identity u is workaround for secretary "name feature"
      (defroute (identity route) {:as params}
        (do-route {:mode mode :mode-conf mode-conf :params params})))))

(defn init-nav[{:keys [app-state app-modes url-composer route-handler
                       server-state?] :as conf}]
  (swap! system merge {:app app-state
                       :app-modes app-modes
                       :url-composer
                       (or url-composer (-> render-url
                                            compose-raw-query
                                            compose-page-number
                                            compose-data-key
                                            ))
                       :route-handler
                       (or route-handler (-> handle-route
                                             handle-data-key
                                             handle-query-params
                                             ))}
         (when-not (nil? server-state?) {:server-state? server-state?}))
  (when-not glo/server-side? (init-history))
  (when app-modes (init-routes app-modes)))


(defn goto [link]
  ;(when event (.preventDefault event))
  (.setToken history (if (= \/(first link)) (subs link 1) link))
  )

(defn url-update [context]
  (goto (url-to (assoc context :url-update? true))))
