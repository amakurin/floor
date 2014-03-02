(ns floor16.ui.utils
  (:require
   [goog.dom :as gd]
   )
  )

(defn el-matcher [el]
  (fn [other] (identical? other el)))

(defn in? [e el]
  (let [target (.-target e)]
    (or (identical? target el)
        (not (nil? (gd/getAncestor target (el-matcher el)))))))
