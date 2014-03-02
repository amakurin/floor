(defproject
  floor16
  "0.1.0-SNAPSHOT"
  :repl-options
  {:init-ns floor16.repl}
  :dependencies
  [
   [org.clojure/core.memoize "0.5.6"]
   [ring-server "0.3.1"]
   [org.clojure/clojurescript "0.0-2156"]
   [environ "0.4.0"]
   ;[com.taoensso/timbre "2.7.1"]
   ;[markdown-clj "0.9.35"]
   ;[com.taoensso/tower "2.0.1"]
   [org.clojure/clojure "1.5.1"]
   [selmer "0.5.8"]
   [compojure "1.1.6"]
   [ring-middleware-format "0.3.2"]
   [liberator "0.10.0"]
   [clauth "1.0.0-rc17"]
   [mysql/mysql-connector-java "5.1.28"]
   [korma "0.3.0-RC6"]
   [org.clojars.amakurin/lobos "1.0.4"]
   [lib-noir "0.7.6"]
   ;[com.postspectacular/rotor "0.1.0"]
   [crypto-random "1.1.0"]
   [com.cemerick/friend "0.2.0"]
   [secretary "0.4.0"]
   [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
   [clj-time "0.6.0"]
   [om "0.5.0"]
   [gered/clj-rhino "0.2.2"]]
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
      :externs ["react/externs/react.js"]
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
    :env {:selmer-dev true}}}
  :url
  "http://example.com/FIXME"
  :aot [#"^((?!lobos.migrations).)*$"]
  :plugins
  [[lein-ring "0.8.10"]
   [lein-environ "0.4.0"]
   [lein-cljsbuild "1.0.2"]
   ]
  :description
  "FIXME: write description"
  :min-lein-version "2.0.0")















