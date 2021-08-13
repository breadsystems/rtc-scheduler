(ns rtc.auth.util
  (:require
   [buddy.hashers :as hash]
   [crypto.random :as crypto]
   [rtc.db :as db]
   [rtc.env :refer [env]]))

(defn tmp-password
  ([num-bytes]
   (crypto/base64 num-bytes))
  ([]
   (tmp-password 16)))

(comment
  (tmp-password)
  (tmp-password 32))

(defn create-first-admin-user! []
  (try
    ;; TODO get env vars from config
    ;; TODO email an invite to redeem instead of printing the pw
    (let [pw (or (:default-admin-password env) (tmp-password))
          email (or (:default-admin-email env) "rtc@tamayo.email")
          pw-hash (hash/derive pw)
          authy-id (:default-authy-user-id env)
          data {:email email
                :pass pw-hash
                :first_name "Admin"
                :last_name "Auto"
                :pronouns "they/them"
                :phone "1234567890"
                :state "WA"
                :is_admin true
                :authy_id authy-id
                :preferences {}}]
      (if (db/get-user-by-email {:email email})
        (println (format "user with email %s exists" email))
        (do
          (db/create-user! data)
          (println "admin email:" email)
          (println "admin password: " pw)
          (println "authy user ID: " authy-id)))
      nil)
    (catch Exception e
      (.getMessage e))))

(comment
  (create-first-admin-user!))
