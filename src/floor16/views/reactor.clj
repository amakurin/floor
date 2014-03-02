(ns floor16.views.reactor
  (:require
   [clojure.java.io :as io]
   [clj-rhino :as js]))

(def js-base-path "resources/public/js/")

(def scope-cache (atom {}))

(defn- add-file [file scope]
    (let [js-code (slurp file)]
      (js/with-context-if-nil nil
                              (fn [ctx]
                                ;; set to interpret (rather than compile) JS to get around 64K limit
                                (.setOptimizationLevel ctx -1)
                                (js/eval scope js-code :ctx ctx)))))

(defn- new-root-scope [js-path & [embedd-react?]]
  ;; ensure that the standard objects aren't sealed since cljs needs to update the Date prototype
  (let [scope (js/with-context-if-nil nil (fn [ctx] (.initStandardObjects ctx)))]
    (when embedd-react?
      (add-file (str js-base-path "react-pre.js") scope)
      (add-file (str js-base-path "react-0.9.0.js") scope)
      (add-file (str js-base-path "react-post.js") scope))
    (add-file js-path scope)
    scope))

(defn- get-root-scope [js-path & [embedd-react?]]
  (let [cached (get @scope-cache js-path)
        lastModified (.lastModified (io/file js-path))]
    (if (> lastModified (or (:lastModified cached) 0))
      (let [scope (new-root-scope js-path embedd-react?)]
        (swap! scope-cache assoc js-path { :scope scope :lastModified lastModified })
        scope)
      (:scope cached))))

(defn render [app state]
  (let [root-sc (get-root-scope (str js-base-path "site2.js") true)
        sc (js/new-scope nil root-sc)]
    (js/eval sc
             (str "var appStateEdn = '" (pr-str state)"';\n"
                  app ".render(appStateEdn);\n"))))
