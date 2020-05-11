(ns rtc.auth.util
  (:require
   [buddy.hashers :as hash]
   [clojure.string :as string]
   [mount.core :refer [defstate]]
   [rtc.db :as db]))

(defn- tmp-password []
  (string/join ""
               (map (fn [_]
                      (rand-nth
                       (str "0123456789"
                            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                            "abcdefghijklmnopqrstuvwxyz"
                            "!@#$%^&*_+-=/")))
                    (range 0 16))))

(defn create-first-admin-user! []
  (try
    (let [pw (or (System/getenv "ADMIN_PASSWORD") (tmp-password))
          pw-hash (hash/derive pw)
          email (or (System/getenv "ADMIN_EMAIL") "rtc@example.com")
          data {:email email :pass pw-hash :is_admin true}]
      (db/create-user! data)
      (println "admin email:" email)
      (println "admin password: " pw)
      {:email email
       :pass  pw})
    (catch Exception e
      (.getMessage e))))

;; Create an admin user on first startup
(defstate admin-user
  :start (create-first-admin-user!))