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
                    {:email "ctamayo+test@protonmail.com"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Test"
                     :last_name "Provider"
                     :pronouns "they/them"
                     :phone "2532229139"
                     :state "WA"
                     :is_admin false
                     :is_provider true}
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
                    {:email "bedap@tamayo.email"
                     :pass (hash/derive "RTCPassword!")
                     :first_name "Bedap"
                     :last_name ""
                     :pronouns "he/him"
                     :phone "1234567890"
                     :state "DC"
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
      :other_access_needs "Popsicles"
      :reason "Personal reasons"
      :state "WA"})
    (appt/create!
     {:provider_id (:id lauren)
      ;; in one hour
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-hour one-hour))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-hour one-hour thirty-minutes))
      :name "Larry"
      :email "prc@example.com"
      :pronouns "they/them"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_access_needs "Things"
      :reason "Personal reasons"
      :state "WA"})
    ;; Create an appt with access needs
    (let [{id :id} (appt/create!
                    {:provider_id (:id shevek)
                     :start_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-day))
                     :end_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-day one-hour))
                     :name "Laura Palmer"
                     :alias ""
                     :email "laura@twinpeaks.email"
                     :pronouns "she/her"
                     :phone ""
                     :ok_to_text true
                     :date_created (c/to-sql-time today-8am)
                     :other_access_needs "Coconut"
                     :reason "Possession by dark spirits"
                     :state "WA"})]
      (-> (sqlh/insert-into :appointment_needs)
          (sqlh/values [{:appointment_id id
                         :need_id "interpretation"
                         :fulfilled false
                         :info "ASL"}
                        {:appointment_id id
                         :need_id "other"
                         :fulfilled false
                         :info "Some other stuff"}])
          (sql/format)
          (d/execute!)))
    (let [{id :id} (appt/create!
                    {:provider_id (:id shevek)
                     :start_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-day one-hour))
                     :end_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-day one-hour thirty-minutes))
                     :name "Dale Cooper"
                     :alias ""
                     :email "damnfinecoffee@fbi.gov"
                     :pronouns "he/him"
                     :phone ""
                     :ok_to_text true
                     :date_created (c/to-sql-time today-8am)
                     :reason "Other"
                     :state "CA"})]
      (-> (sqlh/insert-into :appointment_needs)
          (sqlh/values [{:appointment_id id
                         :need_id "closed_captioning"
                         :fulfilled false
                         :info "CC info"}
                        {:appointment_id id
                         :need_id "other"
                         :fulfilled false
                         :info "Clean room, reasonably priced"}])
          (sql/format)
          (d/execute!)))
    (appt/create!
     {:provider_id (:id lauren)
      ;; in one day
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-day one-hour))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-day one-day one-hour thirty-minutes))
      :name "Josie Packard"
      :alias ""
      :email "prc.1983@example.com"
      :pronouns "she/her"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_access_needs ""
      :reason "Other"
      :state "CA"})
    (appt/create!
     {:provider_id (:id lauren)
      ;; in one day
      :start_time (c/to-sql-time (+ (inst-ms today-8am) (* 2 one-day) one-hour thirty-minutes))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) (* 2 one-day) one-hour one-hour))
      :name "Audrey Horne"
      :alias ""
      :email "audrey@greatnorthern.com"
      :pronouns "she/her"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_access_needs ""
      :reason "Other"
      :state "CA"})
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
      :other_access_needs ""
      :reason "Other"
      :state "CA"})
    (appt/create!
     {:provider_id (:id lauren)
      ;; in one week
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-hour))
      :name "Takver"
      :email "prc.1983@example.com"
      :pronouns "she/her"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_access_needs ""
      :reason "Other"
      :state "CA"})
    (appt/create!
     {:provider_id (:id lauren)
      ;; in eight days
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-day))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-day one-hour))
      :name "Lilith"
      :email "prc.1983@example.com"
      :pronouns "she/her"
      :phone ""
      :ok_to_text true
      :date_created (c/to-sql-time today-8am)
      :other_access_needs ""
      :reason "Other"
      :state "CA"})
    ;; TODO MOAR APPOINZ
    ))

(defn create-test-availabilities! []
  (let [today-8am (doto (Date.) (.setHours 8) (.setMinutes 0))
        lauren (p/email->provider "lauren@tamayo.email")
        ursula (p/email->provider "ursula@tamayo.email")
        shevek (p/email->provider "shevek@tamayo.email")
        bedap  (p/email->provider "bedap@tamayo.email")]
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
     {:provider_id (:id bedap)
      ;; in six days, for 5 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) (* 6 one-day)))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) (* 6 one-day) (* 5 one-hour)))})
    (avail/create!
     {:provider_id (:id shevek)
      ;; in one week, for 8 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week (* 8 one-hour)))})
    (avail/create!
     {:provider_id (:id ursula)
      ;; in eight days, for 6 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-day))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week one-day (* 8 one-hour)))})
    (avail/create!
     {:provider_id (:id bedap)
      ;; in nine days, for 5 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week (* 2 one-day) (* 3 one-hour)))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week (* 2 one-day) (* 5 one-hour)))})
    (avail/create!
     {:provider_id (:id bedap)
      ;; in ten days, for 4 hours
      :start_time (c/to-sql-time (+ (inst-ms today-8am) one-week (* 3 one-day)))
      :end_time (c/to-sql-time (+ (inst-ms today-8am) one-week (* 3 one-day) (* 4 one-hour)))})
    ;; TODO MOAR AVAILZ
    ))

(defn reset-db!! []
  (do
    (d/bind!)
    (tear-it-all-down!!)
    (create-test-users!)
    (create-test-appointments!)
    (create-test-availabilities!)
    (prn "Test data created.")))

(comment
  (p/emails)
  (p/email->provider "ctamayo+test@protonmail.com")
  (u/email->user "coby01@tamayo.email")
  (reset-db!!))
