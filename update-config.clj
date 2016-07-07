;; Мета-скрипт, который используется для обновления конфигурации групп и
;; замены идентификатора задачи на словарь из идентификатора и имени.
;; Имя берется с сайта (пока поддерживается только информатикс).
;;
;; Скрипт читает файл groups.clj и создает файл groups.clj.new
;; Запускать скрипт не обязательно. Файл с описанием групп можно править вручную.

(require 'clojure.pprint)

;; Детали реализации:
;;
;; 1) Ошибки не обрабатываются - мы хотим сразу о них узнавать
;; 2) На информатикс странная система и прежде, чем откроется условие задачи,
;; происходит два редиректа. Их надо выполнить и запомнить куки.
;; 3) У HttpUrlConnection не вызывается disconnect. Достаточно закрыть потоки.
;; http://kingori.co/minutae/2013/04/httpurlconnection-disconnect/
;; http://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html
;; https://scotte.org/2015/01/httpurlconnection-socket-leak/
;; http://stackoverflow.com/questions/4767553/safe-use-of-httpurlconnection

(defn hrequest
  "Sends HEAD request to the server, silently reads response and closes streams.
   Returns instance of HttpUrlConnection."
  ([url] (hrequest nil url))
  ([cookies url]
    (let [c (.openConnection (java.net.URL. url))]
      (doto c
        (.setInstanceFollowRedirects false)
        (.setRequestMethod "HEAD")
        (.setConnectTimeout 5000)
        (.setReadTimeout 5000))
      (when cookies (.setRequestProperty c "Cookie" cookies))
      (slurp (.getInputStream c))
      c)))

(defn login []
  "Sends two requests to informatics.mccme.ru and returns cookies."
  (let [c (hrequest "http://informatics.mccme.ru/course/view.php?id=3")
        cookies
          (->>
            (get (.getHeaderFields c) "Set-Cookie")
            (map (fn [s] ((clojure.string/split s #";") 0)))
            (clojure.string/join "; "))]
    (hrequest cookies (.getHeaderField c "Location"))
    cookies))

(defn request
  "Connects to the server and reads data."
  ([url] (request nil url))
  ([cookies url]
    (let [c (.openConnection (java.net.URL. url))]
      (doto c
        (.setInstanceFollowRedirects false)
        (.setConnectTimeout 5000)
        (.setReadTimeout 5000))
      (when cookies (.setRequestProperty c "Cookie" cookies))
      (slurp (.getInputStream c)))))

(defn title [cookies id]
  "Returns page's title."
  (let [u "http://informatics.msk.ru/mod/statements/view3.php?chapterid="
        s (request cookies (str u id))
        i (clojure.string/index-of s "<title>")
        j (clojure.string/index-of s "</title>")]
    (subs s (+ i (count "<title>")) j)))

(defn fix-group
  "Replaces naked problems' identifiers with maps."
  [cookies group]
  (let [p1 (fn [x] (if (map? x) x {:mccme x :name (title cookies x)}))
        pm (fn [xs] (mapv p1 xs))
        c1 (fn [x] (update x :problems pm))
        cm (fn [xs] (mapv c1 xs))]
    (print (:connects group))
    (update group :contests cm)))

(defn save
  "Pretty prints object to the file."
  [object filename]
  (with-open [w (clojure.java.io/writer filename)]
    (clojure.pprint/pprint object w)))

(defn main []
  (let [cookies (login)
        x (load-file "groups.clj")
        y (mapv (partial fix-group cookies) x)]
    (save y "groups.clj.new")))
(main)
