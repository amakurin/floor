(ns floor16.cookies)

(defn utc-string-expires [v]
  (if (number? v)
    (let [d (js/Date.)
          ed (+(.getTime d)(* v 1000))]
      (.setTime d ed)
      (.toUTCString d))
    v))

(defn cset [kw v & [{:keys [expires path domain secure]}]]
  (let [path (or path "/")
        expires (when expires (utc-string-expires expires))
        v (.encodeURIComponent js/window v)
        n (name kw)
        ck (str n "=" v
                (when path (str "; path=" path))
                (when expires (str "; expires=" expires))
                (when domain (str "; domain=" domain))
                (when secure (str "; secure=" secure))
                )]
    (set! (.-cookie js/document) ck)))

(defn cget [kw]
  (let [n (name kw)
        re (js/RegExp. (str "(?:^|; )" (.replace n #"([\.$?*|{}\(\)\[\]\\\/\+^])" "\\$1") "=([^;]*)"))
        m (.match (.-cookie js/document) re)]
    (when m (aget m 1))))

(defn cremove [kw]
  (cset kw "" {:expires -1}))


