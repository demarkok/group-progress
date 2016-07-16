;; Читает строчку в формате Base64 и декодирует ее
(let [s (read-line)]
  (print (String. (.decode (java.util.Base64/getDecoder) s))))
