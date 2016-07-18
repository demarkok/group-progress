(ns group-progress.main
  (:require (group-progress html http))
  (:gen-class :methods [^:static [lambda [] void]]))

(defn- s3-edn-read [s3client uri]
  (let [o (.getObject s3client (.getHost uri) (subs (.getPath uri) 1))]
    (with-open [r (java.io.PushbackReader.
                    (clojure.java.io/reader (.getObjectContent o)))]
      (clojure.edn/read r))))

(defn- edn-read [cref path]
  (if (clojure.string/starts-with? path "s3")
    (s3-edn-read @cref (java.net.URI. path))
    (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
      (clojure.edn/read r))))

(defn- s3-write-bytes [s3client uri bytes type public-read?]
  (let [meta
          (doto
            (com.amazonaws.services.s3.model.ObjectMetadata.)
            (.setContentLength (count bytes))
            (.setContentType type))
        stream (java.io.ByteArrayInputStream. bytes)
        put-request (com.amazonaws.services.s3.model.PutObjectRequest.
          (.getHost uri) (subs (.getPath uri) 1) stream meta)]
    (.withStorageClass put-request "REDUCED_REDUNDANCY")
    (when public-read?
      (.withCannedAcl put-request
        (com.amazonaws.services.s3.model.CannedAccessControlList/PublicRead)))
    (.putObject s3client put-request)))

(defn- s3-edn-write [s3client uri obj]
  (with-open [s (java.io.ByteArrayOutputStream.)]
    (with-open [w (clojure.java.io/writer s)]
      (binding [*out* w] (print obj)))
    (s3-write-bytes s3client uri (.toByteArray s) "text/plain" false)))

(defn- edn-write [cref path obj]
  (if (clojure.string/starts-with? path "s3")
    (s3-edn-write @cref (java.net.URI. path) obj)
    (with-open [w (clojure.java.io/writer path)]
      (binding [*out* w] (print obj)))))

(defn- save-html [cref path html]
  (if (clojure.string/starts-with? path "s3")
    (s3-write-bytes
      @cref (java.net.URI. path) (.getBytes html "UTF-8") "text/html" true)
    (spit path html)))

(defn- get-html-name [dest group-id]
  (str dest
    (cond
      (clojure.string/ends-with? dest "/") ""
      (clojure.string/ends-with? dest java.io.File/separator) ""
      :else "/")
    group-id
    ".html"))

(defn- doit [cref config dest cache]
  (let [groups (edn-read cref config)
        saved-attempts
          (if (nil? cache) {}
            (try (edn-read cref cache)
              (catch Exception e
                (println "Cache is not found. Will be created.")
                {})))
        ids (set (map :mccme (mapcat :users groups)))
        attempts (group-progress.http/attempts saved-attempts ids)]
    (when cache (edn-write cref cache attempts))
    (doseq [[id html] (group-progress.html/pages groups attempts)]
      (save-html cref (get-html-name dest id) html))))

(defn -lambda []
  (let [s3client (com.amazonaws.services.s3.AmazonS3Client.)
        config "https://o8v.github.io/school/groups.clj"
        bucket "s3://o8v"
        cache "s3://o8v/progress.cache"]
    (doit (delay s3client) config bucket cache)))

(defn -main
  ([config dest] (-main config dest nil))
  ([config dest cache]
    (let [cref
          (delay
            (com.amazonaws.services.s3.AmazonS3Client.
              (.getCredentials
                (com.amazonaws.auth.profile.ProfileCredentialsProvider.))))]
      (doit cref config dest cache)
      (shutdown-agents))))

