(ns floor16.dal.dbf
  (:require [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.string :as s]
            [floor16.dal.db :as db]
            )
  (:import ;[com.linuxense.javadbf DBFReader DBFWriter DBFField]
   [java.io FileOutputStream FileInputStream]))

(defn get-all-fields [dbf-reader]
  (let [cnt (.getFieldCount dbf-reader)]
    (map (fn [i] (keyword (.getName (.getField dbf-reader i)))) (range cnt))))

(defn record-map [fields record]
  (let [cleaned                         ; Convert dates to strings and no whitespace at the end of fields
        (map (fn [v]
               (cond
                (= java.util.Date (type v))
                (let [d (tc/from-date v)]
                  (str (t/year d) "/" (t/month d) "/" (t/day d)))
                (string? v)
                (s/replace v #"\s+$" "")
                :else v))
             (seq record))]
    (apply hash-map (flatten (map vector fields cleaned)))))

(defn get-all-records [dbf-reader]
  "Gets all the remaining records that have not been accessed from the reader.
  Depends on the state of the reader."
  (letfn [(build-record-list [acc dbf-reader]
            (let [next-record (.nextRecord dbf-reader)]
              (if (seq next-record)
                (recur (conj acc next-record) dbf-reader)
                acc)))]
    (build-record-list [] dbf-reader)))

(defn tst []
  (with-open [in (io/input-stream (FileInputStream. "/home/user/ADDROBJ.DBF"))]
    (let [r (com.linuxense.javadbf.DBFReader. in)
          x (.setCharactersetName r "cp866")
          fields (get-all-fields r)
          records (get-all-records r)
          ]
      (doseq [{:keys [AOLEVEL
                      ACTSTATUS
                      CENTSTATUS
                      AOGUID
                      FORMALNAME
                      OFFNAME
                      SHORTNAME
                      CITYCODE
                      POSTALCODE] :as obj} (map record-map (repeat fields) records)]
        (when (and (= ACTSTATUS 1.0)
                   (or ;(= AOLEVEL 1.0)(= AOLEVEL 2.0)(= AOLEVEL 3.0)(= AOLEVEL 7.0)
                       (and (= AOLEVEL 4.0)
                            (not(or (= CENTSTATUS 1.0)
                                (= CENTSTATUS 2.0)
                                (= CENTSTATUS 3.0))))))
          (println "record: "obj (string? FORMALNAME) (str FORMALNAME)(seq FORMALNAME)"\n")
          (db/ins obj)
          )))))

(tst)

;:ACTSTATUS = 1.0
;:AOLEVEL = 4.0 7.0
;:CENTSTATUS = 1.0 2.0 3.0
