(ns rtc.data
  (:require
   [buddy.hashers :as hash]
   [clj-time.coerce :as c]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.appointments.availabilities]
   [rtc.db :as d]
   [rtc.providers.core :as p]
   [rtc.users.core :as u])
  (:import
   [java.util Date]))

(defn create-test-users! []
  (-> (sqlh/insert-into :users)
      (sqlh/values [;; Mix of kin and rad docs
                    {:email "coby01@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Octavia"
                     :last_name "Butler"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :is_admin true
                     :is_provider false}
                    {:email "coby02@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Ursula"
                     :last_name "Le Guin"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :is_admin true
                     :is_provider true}
                    {:email "coby03@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Shevek"
                     :last_name ""
                     :pronouns "he/him"
                     :phone "1234567890"
                     :is_admin false
                     :is_provider true}
                    {:email "lauren@cobytamayo.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Lauren"
                     :last_name "Olamina"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :is_admin false
                     :is_provider true}])
      (sql/format)
      (d/execute!)))


(defn tear-it-all-down!! []
  (d/reset-everything!!)
  (-> ["TRUNCATE availabilities"] ;; no FKs, so we don't need CASCADE
      (d/execute!))
  (-> ["TRUNCATE appointments CASCADE"]
      (d/execute!))
  (-> ["TRUNCATE users CASCADE"]
      (d/execute!)))

(defn create-test-appointments! []
  (let [now (Date.)
        lauren (p/email->provider "lauren@cobytamayo.com")]
    (-> (honeysql.helpers/insert-into :appointments)
        (sqlh/values [{:provider_id (:id lauren)
                       ;; in one hour
                       :start_time (c/to-sql-time (+ (inst-ms now) (* 60 60 1000)))
                       :end_time (c/to-sql-time (+ (inst-ms now) (* 90 60 1000)))
                       :name "Somebody"
                       :email "prc@example.com"
                       :pronouns "they/them"
                       :phone ""
                       :ok_to_text true
                       :date_created (c/to-sql-time now)
                       :other_needs "Popsicles"
                       :reason "Personal reasons"
                       :state "WA"}])
        (sql/format)
        (d/execute!))))

(comment
  (tear-it-all-down!!)
  (do
    (tear-it-all-down!!)
    (create-test-users!))

  (u/email->user "coby01@cobytamayo.com"))