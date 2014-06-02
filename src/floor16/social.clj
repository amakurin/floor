(ns floor16.social
  (:use
   korma.core
   [korma.db :only (defdb)])
  (:require
   [korma.sql.fns :as kf]
   [environ.core :refer [env]]
   [clj-time.coerce :as tco]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [clojure.string :as s]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [org.httpkit.client :as hclient]
   [cheshire.core :as che]
   [floor16.search :as search]
   )
  (:import
   java.net.URLEncoder
   ))

(def baseurl "https://floor16.ru/ads/")
(def imgs-path "/home/floor/catimgs/")
(def social-schedule "1 /10 * * * * *")
(def after-method-sleep 500)

(def sys (atom {:conf nil :cj nil}))

(defn read-conf [soc-mnemo]
  (:conf
   (swap! sys assoc
         :conf
         (first (select :social (where {:mnemo (str soc-mnemo)}))))))

(defn to-param [k v]
  (str
   (if (string? k) k (-> k name (s/replace #"-" "_")))
   "=" v))

(defn to-method [k]
  (-> k name (s/replace #"-" ".")))

(defn urlencode [s]
  (URLEncoder/encode s))

(defn method-url [{:keys [method] :as req}]
  (let [{:keys [owner-id api-url auth-token]} (:conf @sys)
        url (str api-url (to-method method)
                 "?" (to-param :access-token auth-token))
        r (dissoc req :method)]
    (apply str url (map (fn [[k v]] (str "&" (to-param k v))) r))))

(defn process-response [{:keys [status headers body error] :as resp} req & [dont-extract?]]
  (let [parsed (che/parse-string body)]
    (if-let [err (or error (get parsed "error"))]
      (println "Failed, exception: " err)
      (if dont-extract?
        parsed
        (get parsed "response" )))))

(defn method-get [req]
  (let [url (method-url req)
        resp @(hclient/get url)]
    (process-response resp req)))

(defn method-post [req]
  (let [url (method-url (select-keys req [:method]))
        form-params (-> req
                        (dissoc :method))
        {:keys [status headers body error] :as resp}
        @(hclient/post url {:form-params form-params})]
    (process-response resp req)))

(defn upload-file [url f]
  (let [f (clojure.java.io/file f)
        req {:multipart
             [{:name "photo"
               :content f
               :filename (.getName f)}]}
        {:keys [status headers body error] :as resp}
        @(hclient/post url req)]
    (process-response resp (assoc req :method :upload-img) true)))

(defn pub-img [fimg upload-url]
  (let [{server "server" photo "photo" h "hash"}
        (upload-file upload-url fimg)
        uploaded
        (when server (method-get
                      {:method :photos-saveWallPhoto
                       :group-id (-> @sys :conf :owner-id)
                       :server server
                       :photo (urlencode photo)
                       :hash h}))]
    (when (seq uploaded) (get (first uploaded) "id"))))

(defn pub-imgs [imgs]
  (let [{url "upload_url" :as wus}
        (method-get {:method :photos-getWallUploadServer
                      :group-id (-> @sys :conf :owner-id)})]
    (doall
     (map
      (fn [img]
        (Thread/sleep after-method-sleep)
        (pub-img img url))
      imgs))))

(defn price-to-str [price]
  (->> (str price)
       reverse
       (partition-all 3)
       (map #(apply str %))
       (s/join " ")
       reverse
       (apply str)))

(defn compose-str[{:keys [appartment-type total-area floor floors address description price] :as item}]
  (str "Сдается " (s/upper-case appartment-type)
       (when price (str " " (price-to-str price) "руб." ))
       "\n"
       (when total-area (str "Площадь " total-area " кв.м. "))
       (when floor (str "на " floor "-м этаже "))
       (when floors (str floors " " (if floor "этажного дома" "этажном доме")))
       (when address (str "\n\nПО АДРЕСУ: " address))
       (when description (str "\n\nОПИСАНИЕ:\n" description))))

(defn task-handler [& [t opts]]
  (println "Social-task started")
  (doseq [{:keys [seoid imgs appartment-type description] :as ad}
          (->>
           (search/select-resource
            :pub
            {:query {:unpub 0
                     :with-photo true
                     :soc-vk nil
                     :created [kf/pred-> (tf/unparse
                                  (tf/with-zone (tf/formatters :mysql) (tc/default-time-zone))
                                  (tc/minus (tc/now)(tc/days 1)))]
                     } :page 1 :limit 200
             :q-convert #(search/default-converter % search/conf)
             :fields (concat search/by-id-fields [:soc-vk :id])
             :joins search/search-joins
             :order [:pub.created :ASC]
             :post-process #(search/post-process % #{:imgs :description})
             })
           :items)]
    (let [txt (compose-str ad)
          imgs (->> imgs
                    (map #(s/replace % #"^.*/" imgs-path))
                    pub-imgs
                    (remove nil?)
                    (s/join ","))
          url (str baseurl (urlencode seoid))
          attachments (str imgs "," url)
          post-id (get
                   (method-post {:method :wall-post
                                 :message (str txt "\n\n" url)
                                 :owner_id (str "-"(-> @sys :conf :owner-id))
                                 :from_group 1
                                 :attachments attachments
                                 :lang "ru"}) "post_id")]
      (when post-id
        (update :pub
                (set-fields {:soc-vk post-id})
                (where {:id (:id ad)})))
      (println "Published id:" (:id ad) " seoid:" seoid)
      (Thread/sleep after-method-sleep)))
  (doseq [{:keys [soc-vk id] :as unpub} (select :pub (fields :soc-vk :id) (where {:unpub true :soc-vk [not= nil]}))]
    (method-get {:method :wall-delete
                 :owner_id (str "-"(-> @sys :conf :owner-id))
                 :post_id soc-vk
                 })
    (update :pub
            (set-fields {:soc-vk nil})
            (where {:id id}))
    (println "UNPUBLISHED id:" id)
    (Thread/sleep after-method-sleep))
  )

(defn create-task []
  {:id :social-vk-task
   :handler task-handler
   :schedule social-schedule})

(defn schedule-task [cj task]
    (ts/schedule-task (:scheduler cj) (ts/task-entry task)))

(defn start-social []
  (let [cj (:cj (reset! sys {:cj (sched/cronj :entries [])}))
        conf (read-conf :vk)]
    (when conf
      (.start (Thread. task-handler))
      (schedule-task cj (create-task))
      (sched/start! cj))))

