(ns lobos.migrations
  (:refer-clojure
   :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema config))
  (:require
   [floor16.dal.db :as db]))

(defmigration add-users-table
  (up [] (create
          (table :users
                 (varchar :id 20 :primary-key)
                 (varchar :first_name 30)
                 (varchar :last_name 30)
                 (varchar :email 30)
                 (boolean :admin)
                 (time    :last_login)
                 (boolean :is_active)
                 (varchar :pass 100))))
  (down [] (drop (table :users))))

(defmigration init-structure
  (up []
      (create
       (table :cities
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100 :not-null (default ""))
              (decimal :latitude 10 8)
              (decimal :longitude 11 8)
              (boolean :metro :not-null (default false))
              (boolean :districts :not-null (default false))
              ))
      (create
       (table :districts
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              (integer :city [:refer :cities :id])
              (decimal :latitude 10 8)
              (decimal :longitude 11 8)
              ))
      (create
       (table :metro-stations
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              (integer :city [:refer :cities :id])
              (decimal :latitude 10 8)
              (decimal :longitude 11 8)
              (smallint :image-x)
              (smallint :image-y)
              ))
      (create
       (table :streets
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              ))
      (create
       (table :object-types
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              (integer :rooms-count :not-null (default 1))
              ))
      (create
       (table :rent-types
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              ))
      (create
       (table :billing-periods
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              ))
      (create
       (table :building-types
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              ))
      (create
       (table :wc-types
              (integer :id :primary-key :auto-inc)
              (varchar :code 50 :unique)
              (varchar :name 100)
              ))
      (create
       (table :ads
              (integer :id :primary-key :auto-inc)
              (integer :city [:refer :cities :id] :not-null)
              (integer :street [:refer :streets :id] :not-null)
              (varchar :house-no 4 :not-null)
              (varchar :corpus-no 4)
              (varchar :building-no 4)
              (varchar :appartment-no 4)
              (decimal :latitude 10 8)
              (decimal :longitude 11 8)
              (integer :object-type [:refer :object-types :id] :not-null)
              (integer :rent-type [:refer :rent-types :id] :not-null)
              (decimal :price-per-period 8 0 :not-null)
              (integer :billing-period [:refer :billing-periods :id] :not-null)
              (decimal :insurance-deposit 8 0 :not-null (default 0))
              (boolean :can-split-deposit :not-null (default false))
              (boolean :kids-allowed :not-null (default true))
              (boolean :pets-allowed :not-null (default true))
              (integer :building-type [:refer :building-types :id])
              (smallint :floor :not-null (default 1))
              (smallint :building-floors :not-null (default 1))
              (decimal :total-area 8 0 :not-null)
              (decimal :living-area 8 0 :not-null)
              (decimal :kitchen-area 8 0 :not-null)
              (integer :wc-type [:refer :wc-types :id] :not-null)
;-------------------------
              (boolean :washing-machine :not-null (default false))
              (boolean :intenet :not-null (default false))
              (boolean :intercom :not-null (default false))
              (boolean :appliances :not-null (default false))
              (boolean :security :not-null (default false))
              (boolean :concierge :not-null (default false))
              (boolean :smoking :not-null (default false))
              (boolean :bay-window :not-null (default false))
              (boolean :furnished :not-null (default false))
              (boolean :unfurnished :not-null (default false))
              (boolean :phone :not-null (default false))
              (boolean :balcony :not-null (default false))
              (boolean :lift :not-null (default false))
              (boolean :garbage-chute :not-null (default false))
              (boolean :parking :not-null (default false))
              (boolean :loggia :not-null (default false))
              (boolean :air-conditioner :not-null (default false))
              (boolean :refrigerator :not-null (default false))
              (boolean :tv :not-null (default false))

              ))
      (db/default-data))
  (down [] (drop (table :cities))))

;city
;street
;house-no
;corpus-no
;building-no
;appartment-no
;latitude
;longitude
;object-type room studio flat
;room-count
;rent-type short long
;price-per-period
;billing-period
;insurance-deposit
;can-split-deposit
;kids-allowed
;pets-allowed
;building-type brick hru stalin etc.
;floor
;building-floors
;total-area
;living-area
;kitchen-area
;wc-type

;---------------------
;washing-machine
;intenet
;intercom
;appliances bit-pribori
;security
;concierge
;smoking
;bay-window
;furnished
;unfurnished
;phone
;balcony
;lift
;garbage-chute
;parking
;loggia
;air-conditioner
;refrigerator
;tv
