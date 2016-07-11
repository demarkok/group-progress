(ns group-progress.local
	(:require (group-progress core http)))

(require 'clojure.pprint)

(defn -main [groups-path cache-path output-dir]
  (let [cache-file (java.io.File. cache-path)
        cache (if (.isFile cache-file)
          (with-open [r (java.io.PushbackReader.
                          (clojure.java.io/reader cache-file))]
            (clojure.edn/read r))
          {})
        groups (load-file groups-path)
        ids (set (map :mccme (mapcat :users groups)))
				attempts (group-progress.http/attempts cache ids)]
		(with-open [w (clojure.java.io/writer cache-file)]
			(binding [*out* w]
				(clojure.pprint/pprint attempts)))
		(doseq [[id html] (group-progress.core/pages groups attempts)]
			(spit (java.io.File. output-dir (str id ".html")) html))
		(shutdown-agents)))
