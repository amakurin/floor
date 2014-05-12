(ns floor16.http
  (:use ring.util.response
        [clojure.string :only [upper-case join]])
  (:require [ring.middleware.format-response :as ring-format]))

(defn url-from
  [{scheme :scheme server-name :server-name server-port :server-port uri :uri}
    & path-elements]
    (str "http://" server-name ":" server-port  uri "/" (join "/" path-elements)))

(defn options
  ([] (options #{:options} nil))
  ([allowed] (options allowed nil))
  ([allowed body]
    (->
      (response body)
      (header "Allow" (join ", " (map (comp upper-case name) allowed))))))

(defn method-not-allowed
  [allowed]
    (->
      (options allowed)
      (status 405)))

(defn generic-response
  [& [body st]]
  (-> (if body {:body body} {})
      (status (or st 200))))

(defn unauthenticated
  [& [realm]]
  (->
   (generic-response nil 401)
   (header "WWW-Authenticate" (format "Bearer realm=\"%s\"" (or realm "/")))))

(defn no-content?
  [body]
  (if (or (nil? body) (empty? body))
    (->
      (response nil)
      (status 204))
    (response body)))

(defn not-implemented
  []
  (->
    (response nil)
    (status 501)))

(defn gen-error []
  (->
    (response nil)
    (status 500)))

(defn redirect-to [url]
  (redirect url))

(defn wrap-restful-response
  "Wrapper that tries to do the right thing with the response :body
  and provide a solid basis for a RESTful API. It will serialize to
  JSON, YAML, Clojure or HTML-wrapped YAML depending on Accept header.
  See wrap-format-response for more details. Recognized formats are
  :json, :json-kw, :edn :yaml, :yaml-in-html."
  [handler & {:keys [handle-error formats charset predicate]
              :or {handle-error ring-format/default-handle-error
                   charset "utf-8" predicate (fn [req res] (and (not (nil? (:body res))) (ring-format/serializable? req res)))
                   formats [:json :yaml :edn :clojure :yaml-in-html]}}]
  (let [encoders (for [format formats
                       :when format
                       :let [encoder (if (map? format)
                                       format
                                       (get ring-format/format-encoders (keyword format)))]
                       :when encoder]
                   encoder)]
    (ring-format/wrap-format-response handler
                          :predicate predicate
                          :encoders encoders
                          :charset charset
                          :handle-error handle-error)))
