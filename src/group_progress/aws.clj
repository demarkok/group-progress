(ns group-progress.aws
	(:require (group-progress core http))
	(:gen-class :methods [^:static [lambda [] void]]))

(defn- http-groups [url]
	(let [c (.openConnection (java.net.URL. url))]
		(with-open [r (java.io.PushbackReader. (clojure.java.io/reader (.getInputStream c)))]
			(clojure.edn/read r))))

(defn- save-public-html [s3client bucket key data]
	(let [bytes (.getBytes data "UTF-8")
				meta 
					(doto
						(com.amazonaws.services.s3.model.ObjectMetadata.)
						(.setContentLength (count bytes))
						(.setContentType "text/html"))
				stream (java.io.ByteArrayInputStream. bytes)
				put-request
					(doto
						(com.amazonaws.services.s3.model.PutObjectRequest. bucket key stream  meta)
						(.withStorageClass "REDUCED_REDUNDANCY")
						(.withCannedAcl (com.amazonaws.services.s3.model.CannedAccessControlList/PublicRead)))]
		(.putObject s3client put-request)))

(defn -lambda []
  (let [s3client (com.amazonaws.services.s3.AmazonS3Client.)
        groups (http-groups "https://o8v.github.io/school/groups.clj")
        ids (set (map :mccme (mapcat :users groups)))
				attempts (group-progress.http/attempts {} ids)]
		(doseq [[id html] (group-progress.core/pages groups attempts)]
			(save-public-html s3client "o8v" (str id ".html") html))
		(shutdown-agents)))

(defn -main [groups-path cache-path s3bucket]
  (let [s3client (com.amazonaws.services.s3.AmazonS3Client.
					(.getCredentials (com.amazonaws.auth.profile.ProfileCredentialsProvider.)))
				cache-file (java.io.File. cache-path)
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
				(print attempts)))
		(doseq [[id html] (group-progress.core/pages groups attempts)]
			(save-public-html s3client s3bucket (str id ".html") html))
		(shutdown-agents)))

