(ns floor16.resources)


(defprotocol IResource
  (all [this state])
  (by-key [this state k])
  (name-by-key [this state k])
  (d-key [this])
  (d-name [this])
  (by-query [this state query]))


