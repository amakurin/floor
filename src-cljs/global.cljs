(ns floor16.global
  (:require
   [clojure.string :as s]
   [goog.dom :as gd]
   [goog.style :as gs]))

(def server-side? false)

(def ENTER 13)
(def UP_ARROW 38)
(def DOWN_ARROW 40)
(def TAB 9)
(def ESC 27)

(def KEYS #{UP_ARROW DOWN_ARROW ENTER TAB ESC})

(defn key-event->keycode [e]
  (.-keyCode e))

(defn key->keyword [code]
  (condp = code
    UP_ARROW   :prev
    DOWN_ARROW :next
    ENTER      :select
    TAB        :exit
    ESC        :exit))

(defn el-matcher [el]
  (fn [other] (identical? other el)))

(defn in? [e el]
  (let [target (.-target e)]
    (or (identical? target el)
        (not (nil? (gd/getAncestor target (el-matcher el)))))))

(defn node-visible [id vis]
  (when-let [node (gd/$ id)]
    (gs/showElement node vis)))

(defn node-set-props [node props]
  (gd/setProperties node props))

(defn get-offset-vbound [node bound]
  (if (= :top bound) (.-offsetTop node) (+ (.-offsetTop node)(.-offsetHeight node))))

(defn scroll-to-or-top [id & [vbound]]
  (let [vbound (or vbound :top)]
    (if-let [node (gd/$ id)]
      (.scrollTo js/window 0 (get-offset-vbound node vbound))
      (.scrollTo js/window 0 0))))

(defn price-to-str [price]
  (->> (str price)
       reverse
       (partition-all 3)
       (map #(apply str %))
       (s/join " ")
       reverse
       (apply str)))
