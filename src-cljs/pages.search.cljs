(ns floor16.pages.search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [goog.events :as events]
   [goog.events.EventType]
   [cljs.core.async :refer [put! <! chan]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [floor16.ui.generic :as gen]
   [floor16.lang :refer [l]]
   [floor16.auth :as auth]
   )
  )

(defn simple-filter [filt owner]
  (om/component
   (dom/div #js{:className "simple-filter"}
            (om/build gen/select filt
                      (->>{:opts {:data-key :city-id
                                  :placeholder (l :city)
                                  :resource (gen/res-city [:common :refs :cities])}}
                                 (gen/add-filter-chan owner))))))

(defn extended-filter [data owner opts]
  (om/component
   (dom/div #js{:className ""} "extended-filter")))


(defn ad-item-view [item owner opts]
  (om/component
   (dom/div #js{:className ""} "ad-item-view")))


(defn page [cursor owner {:keys [page-id page-path] :as opts}]
  (om/component
   (dom/div #js{:id "sec-home"}
            (dom/div #js{:id "spot" :className "design-layer"})
            (dom/div #js{:id "icons" :className "design-layer"})
            (dom/div #js{:id "cells" :className "design-layer"})
            (dom/div #js{:className "container"}
                     (when (auth/guest?)
                       (dom/div #js{:className "advert"}
                                (dom/div #js{:id "slogan" :className "eight columns"}
                                         (dom/h1 nil
                                                 (dom/img #js {:src "/img/slogan.png"
                                                               :alt "Найди жилье сам! Прямая аренда от собственников!"}))
                                         (dom/p nil
                                                "Ежедневные обновления базы жилых объектов"
                                                (dom/br nil)
                                                "SMS и E-mail рассылка подходящих вариантов"
                                                (dom/br nil)
                                                "Гибкая система тарификации"))
                                (dom/div #js{:id "scheme" :className "offset-by-one six columns"}
                                         (dom/img #js {:src "/img/scheme.png"
                                                       :alt "Удобный поиск. Ежедневные обновления. Автоподбор вариантов. SMS и E-mail рассылка. Онлайн консультации."}
                                                  ))
                                (dom/div #js{:className "clear"})))
                     (let [pg (get-in cursor page-path)]
                       (if (empty? (:filter pg))
                         (om/build simple-filter (:filter pg))
                         (om/build gen/list-view pg
                                   {:opts {:top-filter simple-filter
                                           :side-filter extended-filter
                                           :header-opts {:sort {:view gen/sort-select}
                                                         :views {:view gen/view-select}}
                                           :empty-text (l :empty-search)
                                           :item-view ad-item-view
                                           :res (gen/res-city (conj page-path :data))}})))
                     ))))

