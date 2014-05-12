(ns floor16.global
  (:require
   [goog.dom :as gd]))

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
