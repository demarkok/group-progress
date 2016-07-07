(ns group-progress.core
	(:require group-progress.html))

(defn- problem's-result
	"Calculates problem's result.
	Each attempt is a pair of problem id and verdict."
	[pairs]
	(reduce
		(fn [r [_ a]]
			(if (or (= :ce a) (#{:accepted :solved} (:state r)))
				r
				{:state a :attempts (inc (:attempts r))}))
		{:state :rj :attempts 0}
		pairs))

(defn- mccme-results
  "Converts a map user->attempts into user->problem->results.
	Each attempt is a pair of problem id and verdict."
  [attempts]
	(let [map' (fn [m f] (into {} (map (fn [[k v]] [k (f v)]) m)))
				f0 (fn [pairs] (map' (group-by first pairs) problem's-result))
				f1 (fn [user->attempts] (map' user->attempts f0))]
		(f1 attempts)))

(defn- group's-results [group mccme-results]
  "Converts results from the mccme's domain to the domain of the group."
  (filterv (complement nil?)
    (for [[ui u] (map-indexed vector (:users group))
          [ci c] (map-indexed vector (:contests group))
          [pi p] (map-indexed vector (:problems c))]
      (when-let [res (get-in mccme-results [(:mccme u) (:mccme p)])]
        (assoc res :user ui :contest ci :problem pi)))))

(defn pages
	"Returns map where keys are ids and values are corresponding html pages."
	[groups attempts]
  (let [mr (mccme-results attempts)]
		(into {}
			(for [g groups :let [gr (group's-results g mr)]]
				[(:id g) (group-progress.html/page g gr)]))))
		
