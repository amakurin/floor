(ns floor16.sitemaps
  (:use
   korma.core
   [korma.db :only (defdb)])
  (:require
   [environ.core :refer [env]]
   [clj-time.coerce :as tco]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [clojure.xml :as xml]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   ))

(def sitemap-xmlns "http://www.sitemaps.org/schemas/sitemap/0.9")
(def hostname "https://floor16.ru/")
(def map-path "resources/public/maps/")
(def create-maps-schedule "1 /10 * * * * *")

(defn make-ads-map [{:keys [base-url priority changefreq lim skip-hours]
                 :or {base-url (str hostname "ads/")
                      changefreq "yearly"
                      priority "0.7"
                      lim 9000
                      skip-hours 1}}]
  (let [f (tf/formatter "yyyy-MM-dd'T'HH:mm:ssZZ" (tc/default-time-zone))]
    (->>
     (select :pub
             (fields :created :seoid)
             (where {:unpub 0
                     :created [< (tf/unparse
                                  (tf/with-zone (tf/formatters :mysql) (tc/default-time-zone))
                                  (tc/minus (tc/now)(tc/hours skip-hours)))]})
             (order :created :desc)
             (limit 9000))
     (map (fn [{:keys [created seoid]}]
            {:tag :url
             :content
             [
              {:tag :loc :content [(str base-url seoid)]}
              {:tag :lastmod :content [(tf/unparse f (tco/from-sql-time created))]}
              {:tag :changefreq :content [changefreq]}
              {:tag :priority :content [priority]}
              ]
             }))
     ((fn [x] {:tag :urlset
               :attrs {:xmlns sitemap-xmlns}
               :content
               (concat
                [{:tag :url
                  :content
                  [
                   {:tag :loc :content [base-url]}
                   {:tag :lastmod
                    :content [(tf/unparse
                               (tf/formatter "yyyy-MM-dd'T'HH:mm:ssZZ" (tc/default-time-zone))
                               (tc/now))]}
                   {:tag :changefreq :content ["hourly"]}
                   {:tag :priority :content ["0.9"]}
                   ]}]
                x)}))
     xml/emit with-out-str)))

(defn make-index-map [{:keys [base-url lastmod-static]
                       :or {base-url (str hostname "maps/")
                            lastmod-static "2014-30-05"}}]
  (let [im {:tag :sitemapindex
            :attrs {:xmlns sitemap-xmlns}
            :content
            [{:tag :sitemap
              :content
              [{:tag :loc :content [(str base-url "smstatic.xml")]}
               {:tag :lastmod :content [lastmod-static]}]}
             {:tag :sitemap
              :content
              [{:tag :loc :content [(str base-url "smads.xml")]}
               {:tag :lastmod :content [(tf/unparse
                                         (tf/formatter "yyyy-MM-dd'T'HH:mm:ssZZ" (tc/default-time-zone))
                                         (tc/now))]}]}
             ]}]
    (-> im xml/emit with-out-str)))

(def sys (atom {}))

(defn create-map-file [creator file-name]
  (when creator
    (->> (creator nil)
         (spit (str (or (env :sitemap-path) map-path) file-name)))))

(defn create-maps [& [t opts]]
  (create-map-file make-ads-map "smads.xml")
  (create-map-file make-index-map "index.xml"))

(defn create-task []
  {:id :map-create-task
   :handler create-maps
   :schedule create-maps-schedule})

(defn schedule-task [cj task]
    (ts/schedule-task (:scheduler cj) (ts/task-entry task)))

(defn start-map-gen []
  (let [cj (:cj (reset! sys {:cj (sched/cronj :entries [])}))]
    (create-maps)
    (schedule-task cj (create-task))))
