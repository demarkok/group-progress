(ns group-progress.aws
  (:require (group-progress core http))
  (:gen-class :methods [^:static [lambda [] void]]))

(defn- obj->bytes [o]
  (with-open [s (java.io.ByteArrayOutputStream.)]
    (with-open [w (clojure.java.io/writer s)]
      (binding [*out* w] (print o)))
    (.toByteArray s)))

(defn- uri-read-obj [uri]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader uri))]
    (clojure.edn/read r)))

(defn- uri-write-obj [uri obj]
  (with-open [w (clojure.java.io/writer uri)]
    (binding [*out* w] (print obj))))

(defn- s3-read-obj [s3client uri]
  (let [o (.getObject s3client (.getHost uri) (subs (.getPath uri) 1))]
    (with-open [r (java.io.PushbackReader.
                    (clojure.java.io/reader (.getObjectContent o)))]
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

(defn- s3-write-obj [s3client uri obj]
  (s3-write-bytes s3client uri (obj->bytes obj) "text/plain" false))

(defn- s3-write-html [s3client uri html]
  (s3-write-bytes s3client uri (.getBytes html "UTF-8") "text/html" true))

(defn- read-obj [s3client uri]
  (if (= "s3" (.getScheme uri)) (s3-read-obj s3client uri) (uri-read-obj uri)))

(defn- write-obj [s3client uri obj]
  (if (= "s3" (.getScheme uri))
    (s3-write-obj s3client uri obj)
    (uri-write-obj uri obj)))

(defn- doit [s3client groups-path s3bucket cache-path]
  (let [groups (read-obj s3client (java.net.URI. groups-path))
        cache (if (nil? cache-path)
                {}
                (try
                  (read-obj s3client (java.net.URI. cache-path))
                  (catch Exception e (println "Cache is not found. Will be created.") {})))
        ids (set (map :mccme (mapcat :users groups)))
        attempts (group-progress.http/attempts cache ids)]
    (when cache-path (write-obj s3client (java.net.URI. cache-path) attempts))
    (doseq [[id html] (group-progress.core/pages groups attempts)]
      (s3-write-html
        s3client
        (java.net.URI. "s3" s3bucket (str "/" id ".html") nil)
        html))
    (shutdown-agents)))

(defn -main
  ([groups-path s3bucket] (-main groups-path s3bucket nil))
  ([groups-path s3bucket cache-path]
    (let [credentials (.getCredentials
                        (com.amazonaws.auth.profile.ProfileCredentialsProvider.))
          s3client (com.amazonaws.services.s3.AmazonS3Client. credentials)]
      (doit s3client groups-path s3bucket cache-path))))


;(defn -lambda []
;  (let [s3client (com.amazonaws.services.s3.AmazonS3Client.)
;        groups (http-groups "https://o8v.github.io/school/groups.clj")
;        ids (set (map :mccme (mapcat :users groups)))
;        attempts (group-progress.http/attempts {} ids)]
;    (doseq [[id html] (group-progress.core/pages groups attempts)]
;      (save-public-html s3client "o8v" (str id ".html") html))
;    (shutdown-agents)))
;
;(defn -main [groups-path cache-path s3bucket]
;  (let [s3client (com.amazonaws.services.s3.AmazonS3Client.
;          (.getCredentials (com.amazonaws.auth.profile.ProfileCredentialsProvider.)))
;        cache-file (java.io.File. cache-path)
;        cache (if (.isFile cache-file)
;          (with-open [r (java.io.PushbackReader.
;                          (clojure.java.io/reader cache-file))]
;            (clojure.edn/read r))
;          {})
;        groups
;          (with-open [r (java.io.PushbackReader.
;                          (clojure.java.io/reader groups-path))]
;            (clojure.edn/read r))
;        ids (set (map :mccme (mapcat :users groups)))
;        attempts (group-progress.http/attempts cache ids)]
;    (with-open [w (clojure.java.io/writer cache-file)]
;      (binding [*out* w]
;        (print attempts)))
;    (doseq [[id html] (group-progress.core/pages groups attempts)]
;      (save-public-html s3client s3bucket (str id ".html") html))
;    (shutdown-agents)))
;
;
