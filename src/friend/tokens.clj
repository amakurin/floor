(ns friend.tokens
  (:require
   [crypto.random :as random]
   [clj-time
    [core :as time]
    [coerce :as coerce]]
   ))

(defprotocol Expirable
  (expired? [t]))

(extend-protocol Expirable clojure.lang.IPersistentMap
  (expired? [t] (if-let [expiry (:expires t)]
                   (time/before? (coerce/to-date-time expiry) (time/now))
                   false)))

(extend-protocol Expirable nil
  (expired? [t] false))

(defprotocol ITokenStore
  (insert [store token])
  (fetch-all [store])
  (fetch [store token])
  (delete [store token]))

(deftype InMemoryStore [state]
  ITokenStore
  (insert [_ token] (swap! state conj token))
  (fetch-all [_] @state)
  (fetch [_ token]
         (let [token-filter (if (string? token)
                              #(= token (:identity %))
                              #(= (:identity token) (:identity %)))]
           (->> @state
                (filter token-filter)
                (first))))

  (delete [_ token]
          (let [token-filter (if (string? token)
                               #(= token (:identity %))
                               #(= (:identity token) (:identity %)))]
            (swap! state #(remove (fn [t] (or (token-filter t) (expired? t))) %)))))

(defonce in-memory-store (InMemoryStore. (atom [])))

(defn generate-token-id [] (random/base64 20))

(defn new-token [token-store user-record]
  (let [token {:identity (generate-token-id)
               :username (:username user-record)
               :roles (:roles user-record)
               :expires (-> 1 time/minutes time/from-now coerce/to-string)
               }]
    (insert token-store token)
    token))
