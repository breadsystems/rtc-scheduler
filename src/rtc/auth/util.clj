(ns rtc.auth.util
  (:require
   [buddy.hashers :as hash]
   [clojure.string :as string]
   [mount.core :refer [defstate]]
   [rtc.db :as db]))

(defn- tmp-password []
  (string/join ""
               (map (fn [_]
                      (rand-nth "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"))
                    (range 0 16))))

(defn create-first-admin-user! []
  (try
    (let [pw (or (System/getenv "ADMIN_PASSWORD") (tmp-password))
          pw-hash (hash/derive pw)
          email (or (System/getenv "ADMIN_EMAIL") "rtc-admin@example.com")]
      (db/create-user! {:email email :pass pw-hash})
      (println "admin email:" email)
      (println "admin password: " pw))
    (catch Exception e
      (.getMessage e))))

;; Create an admin user on first startup
(defstate admin-user
  :start (create-first-admin-user!))