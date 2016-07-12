(ns group-progress.html)

(defn- order-users
  "Takes collection of users and sorts it. Returns a lazy seq of pairs.
  The first items of pairs are users and the second ones are places."
  [coll]
  (let [sorted (sort-by (fn [[k v]] [(- (:solved v)) (:attempts v) k]) coll)
        groups (partition-by (fn [[k v]] (:solved v)) sorted)
        prefix-sums (reduce #(conj %1 (+ (last %1) %2)) [0] (map count groups))]
    (apply concat
      (map (fn [g s] (map #(vector (%1 0) (inc s)) g)) groups prefix-sums))))

(defn- g-summary [group uc u]
  (let [score-cell (fn [solved max]
          (if (nil? solved)
            [:td]
            [:td {:class (str "c" (quot (* 10 solved) max))} solved]))]
    [[:h2 (str "Суммарные результаты (обновлено "
            (.toString (.withNano
                          (java.time.LocalDateTime/now
                            (java.time.ZoneId/of "Europe/Moscow"))
                          0))
            ")")]
    [:table
      [:tr
        [:th]
        [:th]
        (map-indexed (fn [i _] [:th [:a {:href (str "#" (inc i))} (inc i)]])
          (:contests group))
        [:th {:title "Задачи"} "Σ"]
        [:th {:title "Попытки по решенным задачам"} "±"]]
      (for [[user place] (order-users u)]
        [:tr
          [:td place]
          [:td {:class "left"} (:name ((:users group) user))]
          (for [i (range (count (:contests group)))]
            (score-cell
              (:solved (uc [user i]))
              (count (:problems ((:contests group) i)))))
          [:td (:solved (u user))]
          [:td (:attempts (u user))]])]]))

(defn- c-summary [group i ucp uc]
  (let [url "http://informatics.mccme.ru/mod/statements/view3.php?chapterid="
        info ((:contests group) i)
        problem-cell (fn [res]
          (if (nil? res)
            [:td]
            (if (= :rj (:state res))
              [:td {:class "rj"} (str "-" (:attempts res))]
              (let [c (if (and (:accept group) (= :solved (:state res)))
                        "ok" "ac")]
                (if (= 1 (:attempts res))
                  [:td {:class (str c " ok0")} "+"]
                  [:td {:class c} (str "+" (dec (:attempts res)))])))))]
    [[:h2 {:id (inc i)} (str (inc i) ". " (:name info))]
    [:table
      [:tr
        [:th]
        [:th]
        (map-indexed
          (fn [j p] [:th [:a {:title (:name p)
                              :href (str url (:mccme p))} (char (+ 65 j))]])
          (:problems info))
        [:th {:title "Задачи"} "Σ"]
        [:th {:title "Попытки по решенным задачам"} "±"]]
      (for [[[user _] place] (order-users (filter #(= i ((key %1) 1)) uc))]
        [:tr
          [:td place]
          [:td {:class "left"}  (:name ((:users group) user))]
          (for [j (range (count (:problems  info)))]
            (problem-cell (ucp [user i j])))
          [:td (:solved (uc [user i]))]
          [:td (:attempts (uc [user i]))]])]]))

(defn- page-as-vector [group results]
  (let [map' (fn [m f] (into {} (map (fn [[k v]] [k (f v)]) m)))
        sum (fn [r]
              (let [s (remove #(= :rj (:state %1)) r)]
                {:solved (count s) :attempts (apply + (map :attempts s))}))
        ucp (map' (group-by (juxt :user :contest :problem) results) first)
        uc (map' (group-by (juxt :user :contest) results) sum)
        u (map' (group-by :user results) sum)]
    [:html {:lang "ru"}
      [:head
        [:meta {:charset "utf-8"}]
        [:style "html{font-size:62.5%}body{font:400 1.6rem/1.5 Tahoma,serif}h1{font-size:2.4rem}h2{font-size:2rem}.ac,.ok,.rj{font-size:1.5rem}table{border-collapse:collapse}td,th{padding:4px 10px;border:1px solid #000;text-align:center}a{text-decoration:none;color:#08c}.left{text-align:left}.ac{color:#070}.ok{color:#b8860b}.rj{color:#b00}.ok0{font-weight:900}.c0{background-color:#a50026}.c1{background-color:#d73027}.c2{background-color:#f46d43}.c3{background-color:#fdae61}.c4{background-color:#fee08b}.c5{background-color:#ffffbf}.c6{background-color:#d9ef8b}.c7{background-color:#a6d96a}.c8{background-color:#66bd63}.c9{background-color:#1a9850}.c10{background-color:#006837}"]
        [:title (:name group)]]
      [:body
        [:h1 (str "Страница группы  «" (:name group) "»")]
        (g-summary group uc u)
        (map-indexed (fn [i _] (c-summary group i ucp uc))
          (:contests group))]]))

(defn- dom [x]
  (cond
    (not (coll? x))
    (str x)

    (keyword? (first x))
    (let [tag (name (first x))
          [attrs children]
            (if (map? (fnext x)) [(fnext x) (nnext x)] [nil (next x)])
          q (fn [s] (str "\"" s "\""))
          a (map (fn [[k v]] (str " " (name k) "=" (q v))) attrs)
          c (flatten (map dom children))]
      (cond
        ;; https://www.w3.org/TR/html5/syntax.html#void-elements
        (#{"area" "base" "br" "col" "embed" "hr" "img" "input" "keygen"
          "link" "meta" "param" "source" "track" "wbr"} tag)
        (str "<" tag (apply str a) ">")
        ;; https://www.w3.org/TR/html5/syntax.html#syntax-tag-omission
        (#{"li" "tr" "th" "td"} tag)
        (str "<" tag  (apply str a) ">" (apply str c))
        (empty? c)
        (str "<" tag (apply str a) "/>")
        :else
        (str "<" tag  (apply str a) ">" (apply str c) "</" tag ">")))

    (not (keyword? (first x)))
    (map dom x)))

(defn page [group results]
  (str "<!doctype html>" (dom (page-as-vector group results))))
