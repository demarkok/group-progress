(ns group-progress.http)

(defn- user-attempts-as-html [mccme-user-id attempts-in-cache]
  "Loads results as a html table. Requests only new results."
  (let [url0 "http://informatics.mccme.ru/moodle/ajax/ajax.php?"
        url1 (str url0 "problem_id=0&objectName=submits&group_id=0&"
                "status_id=-1&lang_id=-1&statement_id=0&action=getPageCount&"
                "count=1&user_id=" mccme-user-id)
        x (Long. (re-find #"\d+" (slurp url1)))]
    (if (= x attempts-in-cache)
      ""
      (slurp (str url0 "problem_id=0&group_id=0&lang_id=-1&status_id=-1&"
                "statement_id=0&objectName=submits&page=0&action=getHTMLTable&"
                "count=" (- x attempts-in-cache) "&user_id=" mccme-user-id)))))

(defn- parse-attempt [s]
  (when-let [x (re-find #"^\d+" s)]
    (when-let [a (clojure.string/index-of s "<\\/a>")]
      (let [c (Long. x)
            f (fn [c] (let [i (int c)] (if (< i 128) c (format "\\u%04x" i))))
            e (fn [s] (apply str (map f s)))]
        (cond
          (clojure.string/index-of s  (e "/Принято") a)
          [c :accepted]
          (clojure.string/index-of s "OK" a)
          [c :solved]
          (clojure.string/index-of s  (e "Ошибка компиляции") a)
          [c :ce]
          :else
          [c :rj])))))

(defn- user's-attempts
  "Returns a chronlogically ordered  list of user's attempts. Each item
  in the list is a pair of problem id and verdict."
  [cache mccme-user-id]
  (let [cached (get cache mccme-user-id)
        html (user-attempts-as-html mccme-user-id (count cached))
        coll (reverse (drop 1 (clojure.string/split html #"chapterid=")))]
    (vec (concat cached (keep parse-attempt coll)))))

(defn attempts
  "Returns a map of attempts by adding new attempts for the specified users
  to the cache. Cache is used to reduce traffic and number of GET requests.
  Requests are performed in parallel and consumer should consider using
  function shutdown-agents at the end of a calling script."
  [cache mccme-ids]
  (into cache (pmap (fn [x] [x (user's-attempts cache x)]) mccme-ids)))
