(ns rtc.data
  (:require
   [buddy.hashers :as hash]
   [clj-time.coerce :as c]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.appointments.availabilities :as avail]
   [rtc.appointments.appointments :as appt]
   [rtc.db :as d]
   [rtc.providers.core :as p]
   [rtc.users.core :as u]
   [rtc.util :refer [thirty-minutes one-hour six-hours one-day one-week]])
  (:import
   [java.util Date]))

(defn create-test-users! []
  (-> (sqlh/insert-into :users)
      (sqlh/values [;; Mix of kin and rad docs
                    {:email "octavia@tamayo.email"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Octavia"
                     :last_name "Butler"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :state ""
                     :is_admin true
                     :is_provider false}
                    {:email "ursula@tamayo.email"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Ursula"
                     :last_name "Le Guin"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :state "WA"
                     :is_admin true
                     :is_provider true}
                    {:email "shevek@tamayo.email"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Shevek"
                     :last_name ""
                     :pronouns "he/him"
                     :phone "1234567890"
                     :state "WA"
                     :is_admin true
                     :is_provider true}
                    {:email "lauren@tamayo.email"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Lauren"
                     :last_name "Olamina"
                     :pronouns "she/her"
                     :phone "1234567890"
                     :state "CA"
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
  (let [today-8am (doto (Date.) (.setHours 8) (.setMinutes 0))
        lauren (p/email->provider "lauren@tamayo.email")
        shevek (p/email->provider "shevek@tamayo.email")]
    (appt/create!
     {:provider_id (:id lauren)
      ;; in one hour
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-hour))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-hour thirty-minutes))
      :name "Somebody"
      :email "prc@example.com"
      :pronouns "they/them"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_needs "Popsicles"
      :reason "Personal reasons"
      :state "WA"})
    (appt/create!
     {:provider_id (:id shevek)
      ;; in one day
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-day))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-hour))
      :alias "Anon"
      :email "prc.1983@example.com"
      :pronouns "she/her"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_needs ""
      :reason "Other"
      :state "CA"})
    ;; TODO MOAR APPOINZ
    ))

(defn create-test-availabilities! []
  (let [today-8am (doto (Date.) (.setHours 8) (.setMinutes 0))
        lauren (p/email->provider "lauren@tamayo.email")
        ursula (p/email->provider "ursula@tamayo.email")
        shevek (p/email->provider "shevek@tamayo.email")]
    (prn "creating test availabilities starting" today-8am)
    (avail/create!
     {:provider_id (:id lauren)
      ;; in one day, for 6 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-day))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-day six-hours))})
    (avail/create!
     {:provider_id (:id lauren)
      ;; in two days, for 8 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) (* 2 one-day)))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) (* 2 one-day) (* 8 one-hour)))})
    (avail/create!
     {:provider_id (:id shevek)
      ;; in one week, for 8 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week (* 8 one-hour)))})
    (avail/create!
     {:provider_id (:id ursula)
      ;; in one week, for 8 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-day))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-day (* 8 one-hour)))})
    ;; TODO MOAR AVAILZ
    ))

(comment
  (do
    (d/bind!)
    (tear-it-all-down!!)
    (create-test-users!)
    (create-test-appointments!)
    (create-test-availabilities!)
    (prn "Test data created."))

  (u/email->user "coby01@tamayo.email"))