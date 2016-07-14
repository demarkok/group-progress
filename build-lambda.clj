(defn delete-recursively [node]
  (when (.exists node)
    (when (.isDirectory node)
      (doseq [f (.listFiles node)] (delete-recursively f)))
    (clojure.java.io/delete-file node)))

(defn zip-dir
  "Zip content of the directory (ignore empty sub-directories)"
  [dir archive-name]
  (let [node (clojure.java.io/file dir)
        abs (.getAbsolutePath node)
        len (+ (count abs)
              (if (clojure.string/ends-with? abs java.io.File/separator) 0 1))]
    (with-open [zip (java.util.zip.ZipOutputStream.
                  (clojure.java.io/output-stream archive-name))]
      (doseq [f (file-seq node) :when (.isFile f)]
        (.putNextEntry zip
          (java.util.zip.ZipEntry. (subs (.getAbsolutePath f) len)))
        (clojure.java.io/copy f zip)
        (.closeEntry zip)))))

(delete-recursively (clojure.java.io/file "target/group_progress"))
(binding [*compile-path* "target"]
         (compile 'group-progress.aws))
(zip-dir "target" "lambda.zip")

