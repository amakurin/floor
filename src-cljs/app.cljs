(ns floor16.app
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
   [cljs.core.async :refer [put! <! chan]]
   [clojure.string :as st]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [goog.events :as events]
   [goog.events.EventType]
   [floor16.ui.generic :as gen]
   [floor16.auth :as auth]
   [floor16.navigation :as nav]
   [floor16.pages.search]
   )
  )

(enable-console-print!)

;; (def app-state (atom {:roles {:leaser {:title "Арендатор"}
;;                               :householder {:title "Домовладелец"}}
;;                       :pages {:search {:filter {}
;;                                        :data {}}}
;;                       }))
;; (defn fake-page [value]
;;   (fn page [state owner opts] (om/component (dom/div #js{} value))))

;; (def nav-conf {:app app-state
;;               :pages [{:id :search
;;                        :component floor16.pages.search/page
;;                        :link-title "Поиск квартир"
;;                        :uri-aliases ["" "!/search/:query"]
;;                        }
;;                       {:id :object
;;                        :component (fake-page "Просмотр объекта")
;;                        :uri "!/:city-code/:area-type-code/:area-code/:obj-type-code/:obj-id"
;;                        :link-title "Подробнее"
;;                        }
;;                       {:id :to-realtors
;;                        :component (fake-page "Риэлторам")
;;                        :link-title "Риэлторам"
;;                        }
;;                       {:id :to-hh
;;                        :component (fake-page "Домовладельцам")
;;                        :link-title "Домовладельцам"
;;                        }
;;                       {:id :profile
;;                        :component (fake-page "Профиль")
;;                        :link-title "Профиль"
;;                        }
;;                       {:id :own-objects
;;                        :component (fake-page "Мои объекты")
;;                        :link-title "Мои объекты"
;;                        }
;;                       {:id :realtor-objects
;;                        :component (fake-page "Моя база объектов")
;;                        :link-title "Моя база объектов"
;;                        }
;;                       ]
;;               :main-page :search
;;               :main-menu {:pages [:search :to-realtors :to-hh]}
;;               :user-menu {:pages [:profile :realtor-objects :own-objects] :draw-icons? true}
;;               :user-menu-top {:pages [:profile :own-objects]}})


;; (defn try-login [cursor owner]
;;   (om/set-state! owner [:error] false)
;;   (auth/do-login {:username (.-value (om/get-node owner "username"))
;;                          :password (.-value (om/get-node owner "pwd"))}
;;                  #(om/set-state! owner [:active] false)
;;                  #(om/set-state! owner [:error] true)))

;; (defn login-keydown [cursor owner e node-ref]
;;   (when (= 13 (.-keyCode e))
;;     (if (= node-ref "username")
;;       (.focus (om/get-node owner "pwd"))
;;       (try-login cursor owner))))

;; ;;; Components

;; (defn logout-item [app owner]
;;   (om/component
;;    (dom/li #js {:key "exit-link" :className "exit-link"}
;;            (dom/span #js {:onClick #(auth/do-logout)} "Выход"))))
;; (println (nav/get-menu :main-menu))


;; (defn menu [app owner {:keys [menu-id add-items]}]
;;   (om/component
;;    (let [{:keys [pages draw-icons?]} (nav/get-menu menu-id)]
;;      (apply dom/ul #js {:id (name menu-id) :className (when draw-icons? "with-icon")}
;;             (concat
;;              (map (fn [{:keys [id uri link-title]}]
;;                     (dom/li #js {:key id :className (if (nav/current? id) "current" "")}
;;                             (dom/a #js{:href uri :title link-title
;;                                        :className (str "menu-link " (name id))} link-title)))
;;                   pages)
;;              (map #(om/build % app) add-items))))))

;; (defn user-login [app owner]
;;   (let [set-active! (fn [value] (om/set-state! owner[:active] value))
;;         set-error! (fn [value] (om/set-state! owner[:error] value))]
;;     (reify
;;       om/IInitState
;;       (init-state [this]
;;                   {:active false
;;                    :error false
;;                    })

;;       om/IDidMount
;;       (did-mount [this]
;;                  (let [mouse-handler
;;                        (fn [e]
;;                          (when (om/get-state owner [:active])
;;                            (when-not (gen/in? e (om/get-node owner))
;;                              (set-active! false)
;;                              (set-error! false))))]
;;                    (events/listen js/document.body goog.events.EventType.MOUSEDOWN mouse-handler)
;;                    (om/set-state! owner [:mouse-handler] mouse-handler)))

;;       om/IDidUpdate
;;       (did-update [this prev-props prev-state]
;;                   (when-let [un-input (om/get-node this "username")] (.focus un-input)))

;;       om/IWillUnmount
;;       (will-unmount [this]
;;                     (if-let [mouse-handler (om/get-state owner [:mouse-handler])]
;;                       (events/unlisten js/document.body goog.events.EventType.MOUSEDOWN mouse-handler)))
;;       om/IRenderState
;;       (render-state [_ {active? :active, error? :error}]
;;                     (dom/div #js{:id "user-login" :className (if active? "active" "")}
;;                              (when (auth/guest?)
;;                                (dom/span #js{:id "login-link" :title "Вход"
;;                                              :onClick (fn [e] (set-active! (not active?)))} (str "Вход" (when active? ":"))))
;;                              (when (and (auth/guest?) active?)
;;                                (dom/div #js{:className "five columns login"}
;;                                         (dom/input #js{:ref "username"
;;                                                        :type "text"
;;                                                        :placeholder "Телефон..."
;;                                                        :onKeyDown #(login-keydown app owner % "username")
;;                                                        })
;;                                         (dom/input #js{:ref "pwd"
;;                                                        :type "password"
;;                                                        :placeholder "Пароль..."
;;                                                        :onKeyDown #(login-keydown app owner % "pwd")})
;;                                         (dom/span #js{:className "enter-link"
;;                                                       :onClick #(try-login app owner)}
;;                                                   "Войти")
;;                                         (when error? (dom/span #js{:className "error"} "Неверные учетные данные."))
;;                                         (when error? (dom/span #js{:className "error link"} "Забыли пароль?"))
;;                                         )))))))

;; (defn user-panel [app owner]
;;   (reify
;;     om/IRenderState
;;     (render-state [_ {active? :active}]
;;                   (let [{:keys [fname lname roles]} (auth/get-user)]
;;                     (dom/nav #js {:id "user-panel"
;;                                   :className (if active? "five columns active" "closed")
;;                                   :onClick #(when (not active?) (om/set-state! owner [:active] true))}
;;                              (if active?
;;                                (into-array
;;                                 (vector
;;                                  (dom/span #js {:className "close"
;;                                                 :onClick #(om/set-state! owner [:active] false)})
;;                                  (dom/img #js {:id "user-pic"
;;                                                :src "/img/user.png"})
;;                                  (dom/div #js {:id "user-info"}
;;                                           (dom/span #js{:id "user-name"} (str fname " " lname))
;;                                           (dom/span #js {:id "user-roles"}
;;                                                     (st/join ", " (map #(get-in app [:roles % :title]) roles))))
;;                                  (om/build menu app {:opts {:menu-id :user-menu :add-items [logout-item]}})
;;                                  ))
;;                                "☰"))))))

;; (defn app-header [app owner]
;;   (om/component
;;    (dom/nav #js{:id "menu" :className "container"}
;;             (om/build menu app {:opts {:menu-id :main-menu}})
;;             (if (auth/guest?)
;;               (om/build user-login app)
;;               (om/build menu app {:opts {:menu-id :user-menu-top :add-items [logout-item]}}))
;;             (dom/a #js{:id "logo" :href "#" :title "Перейти к началу сайта"}
;;                    (dom/img #js{:src "/img/logo.png" :alt "Проект &quotЭТАЖ 16&quot"}))
;;             (when (not (auth/guest?))(om/build user-panel app {:init-state {:active (not(auth/autologin?))}})))))

;; (defn app-main [app owner]
;;   (om/component
;;    (let [{:keys [id component]} (nav/curr-page)]
;;      (om/build component app {:opts {:page-id id :page-path [:pages id]}}))))

;; (defn ^:export main []
;;   (om/root app-header app-state {:target (.getElementById js/document "header")})
;;   (om/root app-main app-state {:target (.getElementById js/document "content")})
;;   )

;; (def as (atom {}))

;; (defn get-server-state []
;;   (when-let [data (.-serverState js/window)]
;;     (cljs.reader/read-string data)))

;; (defn test-app [app owner]
;;   (om/component
;;    (dom/div nil
;;             (dom/span #js{:onClick #(om/update! app :text "Omg! I Am Clicked!")} (:text app))
;;             (dom/span #js{:onClick #(om/update! app :text "Omg! I Am Clicked Tooo!")} "I Am Just Static Text :0)")
;;             )))

;; (defn ^:export main1 []
;;   (reset! as (get-server-state))
;;   (nav/nav-sys nav-conf)
;;   (auth/auth-sys {:app app-state :api-url "oauth/token"})
;;   (println (om/to-cursor as))
;;   (om/root test-app as {:target (.getElementById js/document "content")})
;;   )
;; (defn ^:export render [data]
;;   (let [data (cljs.reader/read-string data)]
;;     (.renderComponentToString js/React (om/build test-app (om/to-cursor data)))))
