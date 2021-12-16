(ns rtc.notifier.reminders
  (:require
    ;; HoneySQL 2.x
    [honey.sql :as sql]
    [mount.core :refer [defstate]]
    [rtc.db :as db]
    [rtc.env :refer [env]]
    [rtc.notifier.api :as api]
    [rtc.notifier.appointments :as appt]))

(defn- get-imminent-appointments []
  (db/query
     (sql/format
      {:select [:appt.start_time :appt.email :appt.phone :appt.ok_to_text
                :appt.reminded_careseeker :appt.reminded_provider
                [:p.first_name :provider_first_name]
                [:p.last_name :provider_last_name]
                [:p.email :provider_email] [:p.phone :provider_phone]]
       :from [[:appointments :appt]]
       :join [[:users :p] [:=  :p.id :appt.provider_id]]
       :where [:and
               [:> :appt.start_time [:now]]
               [:< :appt.start_time
                [:raw "now() + interval '24 hours'"]]
               [:or
                [:= :appt.reminded_careseeker false]
                [:= :appt.reminded_provider false]]]})))

(defmulti send! identity)

(defmethod send! :appointments [_]
  (let [imminent (get-imminent-appointments)
        reminders
        (mapcat (juxt appt/appointment->reminder-sms
                      appt/appointment->provider-reminder-sms
                      #_#_ ;; TODO
                      appt/appointment->reminder-email
                      appt/appointment->provider-reminder-email)
                imminent)]
    reminders #_
    (doseq [reminder reminders]
      (api/notify! reminder))))

(defn -main [task & _]
  (send! (keyword task)))


(comment
  (map (juxt :phone :provider_phone) (get-imminent-appointments))

  (db/query
     (sql/format
      {:select [:appt.start_time :appt.email :appt.phone :appt.ok_to_text
                :appt.reminded_careseeker :appt.reminded_provider
                [:p.email :provider_email] [:p.phone :provider_phone]]
       :from [[:appointments :appt]]
       :join [[:users :p] [:=  :p.id :appt.provider_id]]
       :where [:and
               [:> :appt.start_time [:now]]
               [:< :appt.start_time
                [:raw "now() + interval '24 hours'"]]
               [:or
                [:= :appt.reminded_careseeker false]
                [:= :appt.reminded_provider false]]]}))

  (-main "appointments")

  ;;
  )
