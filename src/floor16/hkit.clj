(ns floor16.hkit
  (:use org.httpkit.server
        [ring.middleware file-info file])
  (:require [floor16.handler :refer [app init destroy]]))

(defonce server (atom nil))

(defn get-handler []
  (-> #'app
    (wrap-file "resources")
    (wrap-file-info)))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (when destroy (destroy))
    (reset! server nil)))

(defn start-server [& [port ip]]
  (when init (init))
  (reset! server (run-server (get-handler) {:port (or port 8080) :ip (or ip "127.0.0.1")}))
  (println (str "Http-kit started: You can view the site at http://localhost:" (or port 8080))))

(defn -main [& [port]]
  (let [port (when port (Integer/parseInt port))]
    (start-server port)
    ))

;; (start-server)
;; (stop-server)
