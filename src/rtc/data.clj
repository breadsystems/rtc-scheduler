(ns rtc.data
  (:require
   [buddy.hashers :as hash]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.appointments.availabilities]
   [rtc.db :as d]
   [rtc.users.core :as u]))

(defn create-test-users! []
  (-> (sqlh/insert-into :users)
      (sqlh/values [;; Mix of kin and rad docs
                    {:email "coby01@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Octavia"
                     :last_name "Butler"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :is_admin true}
                    {:email "coby02@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Ursula"
                     :last_name "Le Guin"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :is_admin true}
                    {:email "coby03@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Shevek"
                     :last_name ""
                     :pronouns "he/him"
                     :phone "1234567890"
                     :is_admin false}
                    {:email "coby04@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Lauren"
                     :last_name "Olamina"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :is_admin false}])
      (sql/format)
      (d/execute!)))


(defn tear-it-all-down!! []
  (-> ["TRUNCATE availabilities"] ;; no FKs, so we don't need CASCADE
      (d/execute!))
  (-> ["TRUNCATE appointments CASCADE"]
      (d/execute!))
  (-> ["TRUNCATE providers CASCADE"]
      (d/execute!))
  (-> ["TRUNCATE users CASCADE"]
      (d/execute!)))

(comment
  (tear-it-all-down!!)
  (do
    (tear-it-all-down!!)
    (create-test-users!))

  (u/email->user "coby01@cobytamayo.com"))