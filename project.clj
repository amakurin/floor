(defproject
  floor16
  "0.1.0-SNAPSHOT"
  :repl-options
  {:init-ns floor16.repl}
  :dependencies
  [
   [org.clojure/core.memoize "0.5.6"]
   [ring-server "0.3.1"]
   [org.clojure/clojurescript "0.0-2173" :scope "provided"]
   [org.clojure/tools.reader "0.8.3"]
   [org.clojure/clojure "1.5.1"]
   [selmer "0.5.8"]
   [compojure "1.1.6"]
   [ring-middleware-format "0.3.2"]
   [org.clojars.r0man/environ "0.4.1-SNAPSHOT"]
   [mysql/mysql-connector-java "5.1.28"]
   [korma "0.3.0-RC6"]
   [clj-sql-up "0.3.1"]
   [crypto-random "1.1.0"]
   [com.cemerick/friend "0.2.0"]
   [secretary "1.0.2"]
   [org.clojure/core.async "0.1.267.0-0d7780-alpha" :scope "provided"]
   [clj-time "0.6.0"]
   [om "0.6.2"]
   [gered/clj-rhino "0.2.2"]
   [com.linuxense/javadbf	"0.4.0"]
   [im.chit/cronj "1.0.1"]
   [http-kit "2.1.18"]]
  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src-cljs"],
     :compiler
     {:optimizations :none,
      :source-map true,
      :pretty-print true,
      :output-to "resources/public/js/site.js",
      :output-dir "resources/public/out",
      :closure-warnings {:non-standard-jsdoc :off}
      }}
    {:id "dev2"
     :source-paths ["src-cljs"],
     :compiler
     {:optimizations :whitespace,
      ;:source-map "resources/public/js/site2.js.map",
      :pretty-print true,
      :output-to "resources/public/js/site2.js",
      :output-dir "resources/public/js/out",
      ;:preamble [;"js/react-prelude.js"
      ;           "react/react.min.js"
                 ;"js/react-postlude.js"
      ;           ]
      :externs ["react/externs/react.js" "google_maps_api_v3.js"]
      :closure-warnings {:non-standard-jsdoc :off}
      }}
    {:id "prod"
     :source-paths ["src-cljs"],
     :compiler
     {:optimizations :advanced,
      :pretty-print false,
      :output-to "resources/public/js/site-prod.js",
      :preamble ["react/react.min.js"]
      :externs ["react/externs/react.js" "externs/google_maps_api_v3.js"]
      :closure-warnings {:non-standard-jsdoc :off}
      }}
    ]}
  :ring
  {:handler floor16.handler/app,
   :init floor16.handler/init,
   :destroy floor16.handler/destroy}
  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}},
   :dev
   {:ring {:auto-reload? true, :reload-paths ["src" "resources"]}
    :dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.2.1"]],
    :env {:dev-debug true
          }
;          :api-url "/api"
;          :img-server-url "http://img.floor16.loc/"
;          :default-select-limit 20
;          :database {:subprotocol "mysql"
;                   :subname "//localhost/caterpillar"
;                   :user "caterpillar"
;                   :password "111111"
;                   :delimiters "`"}}
    }}
  :url
  "http://example.com/FIXME"
  :plugins
  [[lein-ring "0.8.10"]
   [lein-environ "0.4.0"]
   [lein-cljsbuild "1.0.2"]
   ]
  :description
  "FIXME: write description"
  :min-lein-version "2.0.0"
  :jvm-opts ["-Xms512m" "-Xmx1024m"]
  :main floor16.hkit)















