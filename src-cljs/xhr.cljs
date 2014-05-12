(ns floor16.xhr
  (:require-macros [clojure.core :refer [some-> some->>]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [cljs.core.async :refer [put! <! >! chan]]
            [clojure.string :as string])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(defn edn->map [t]
  (when (and t (not (empty? t)))
    (reader/read-string t)))

(defn map->edn [m]
  (when m (pr-str m)))

(defn url-encode
  [string]
  (some-> string str (js/encodeURIComponent) (.replace "+" "%20")))

(defn url-decode
  [string]
  (some-> string str (js/decodeURIComponent)))

(defn map->query
  [m]
  (some->> (seq m)
    sort
    (map (fn [[k v]]
           [(url-encode (name k))
            "="
            (url-encode (str v))]))
    (interpose "&")
    flatten
    (apply str)))

(defn split-param [param]
  (->
   (string/split param #"=")
   (concat (repeat ""))
   (->>
    (take 2))))

(defn query->map
  [qstr]
  (when qstr
    (some->> (string/split qstr #"&")
      seq
      (mapcat split-param)
      (map url-decode)
      (apply hash-map))))

(def ^:private meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(def ^:private med-types
  {:edn {:value "application/edn" :to-map-encoder edn->map  :from-map-encoder map->edn}
   :url-encode {:value "application/x-www-form-urlencoded" :to-map-encoder query->map  :from-map-encoder map->query}})

(def ^:private heads
  {:accept {:value "Accept" :decoder #(get-in med-types [% :value])}
   :content-type {:value "Content-Type" :decoder #(get-in med-types [% :value])}
   :authorization {:value "Authorization" :decoder identity}})

(defn format-headers [headers]
  (->> headers
       (map (fn [[k v]] [(get-in heads [k :value])
                         ((get-in heads [k :decoder]) v)]))
       (into {})
       (clj->js)))

(defn pre-process [{:keys [url method body headers] :as request}]
  (let [content-type (get headers :content-type :edn)
        {body-encoder :from-map-encoder} (content-type med-types)
        accept (get headers :accept :edn)
        headers (format-headers (-> headers
                                    (assoc :content-type content-type)
                                    (assoc :accept accept)))]
    {:method (meths method)
     :url url
     :body (when body (body-encoder body))
     :headers headers}
    ))

(defn create-response [xhr]
  (let [raw-body (.getResponseText xhr)]
    {:status (.getStatus xhr)
     :headers (.getAllResponseHeaders xhr)
     :body (when (and raw-body (not (empty? raw-body))) (reader/read-string raw-body))}))

(defn cb-request [request on-complete]
  (let [xhr (XhrIo.)
        {:keys [url method body headers]} (pre-process request)]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e](on-complete (create-response xhr))))
    (. xhr (send url method body headers))))

(defn chan-request [request ch]
  (let [xhr (XhrIo.)
        {:keys [url method body headers]} (pre-process request)]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e] (put! ch (create-response xhr))))
    (. xhr (send url method body headers))))

(defn accept [request t]
  (assoc-in request [:headers :accept] t))

(defn content-type [request t]
  (assoc-in request [:headers :content-type] t))

(defn bearer-authorization [request token]
  (assoc-in request [:headers :authorization] (str "Bearer " token)))

