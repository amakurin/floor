(ns floor16.navigation
  (:require-macros [secretary.macros :refer [defroute]])
  (:require [om.core :as om :include-macros true]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.events.EventType])
  (:import [goog History]
           [goog.history EventType])
  )

(enable-console-print!)

(def sys-data (atom {:handled-url nil
                     :pages {}}))

(defn app [] (:app @sys-data))

(defn get-cp [] (get-in @(app) [::nav ::current-page]))

(defn set-cp! [page-id] (swap! (app) assoc-in [::nav ::current-page] page-id))

(defn handle-history-event [token]
  (println "history event: " token)
  (if (:handled-url @sys-data)
    (swap! sys-data dissoc :handled-url)
    (secretary/dispatch! token)))

;(def history nil);(History.))

(defn do-route [uri {:keys [id]:as page} params]
  (println "Route: " uri " param: " params )
  (set-cp! id))

(defn nav-sys [{:keys [pages]:as conf}]
  (swap! sys-data #(reduce (fn [res [k v]] (if (= k :pages) res (assoc res k v))) % conf))
  (swap! sys-data assoc :history (History.))
  (doseq [{:keys [id uri uri-aliases] :as p} pages]
    (doseq [u (conj (or uri-aliases []) (or uri (str "!/" (name id))))]
      (defroute u {:as params} (do-route u p params)))
    (swap! sys-data assoc-in [:pages id] (assoc p :uri (str "#" (or uri (str "!/" (name id)))))))
  (let [history (:history @sys-data)]

    (.setEnabled history true)
    (events/listen history EventType/NAVIGATE #(handle-history-event (.-token %)))
    (handle-history-event (.getToken history))
    ))

(defn set-url [url title]
  (swap! sys-data assoc :handled-url url)
  (.setToken (:history @sys-data) url title))

;;;API
(defn get-page [page] (get-in @sys-data [:pages page]))

(defn get-pages [pages] (map #(get-page %) pages))

(defn get-menu [id]
  (let [{:keys [pages] :as menu} (id @sys-data)]
    (assoc menu :pages (get-pages pages))))

(defn curr-page []
  ((get-cp) (:pages @sys-data)))

(defn current? [page-id] (= page-id (get-cp)))

(defn go-page [{page-id :page, data :data, title :title}]
  (println "gopage: " page-id)
  (let [{:keys [uri page-title]} (get-page page-id)]
    (set-url uri (or title page-title))
    (set-cp! page-id)
    ))
(defn go-detail [url-data])
(defn detail-page? [])
(defn ready-state? [])
(defn can-go-back? [])
(defn go-back [])
(defn master-prev [])
(defn master-next [])
