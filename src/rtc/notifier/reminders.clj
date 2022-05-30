(ns rtc.notifier.reminders
  (:require
    ;; HoneySQL 2.x
    [honey.sql :as sql]
    [mount.core :as mount :refer [defstate]]
    [rtc.db :as db]
    [rtc.env :refer [env]]
    [rtc.notifier.api :as api]
    [rtc.notifier.appointments :as appt]))

(defn- get-imminent-appointments []
  (db/query
     (sql/format
      {:select [:appt.id :appt.start_time
                :appt.email :appt.phone :appt.ok_to_text
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

(defn- record-reminder! [{:keys [id]}]
  (db/execute!
    ["UPDATE appointments
     SET reminded_careseeker = true,
         reminded_provider = true
     WHERE id = ?" id]))

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
    (doseq [reminder reminders]
      (api/notify! reminder))
    (doseq [appt imminent]
      (record-reminder! appt))))

(defn -main [task & _]
  (mount/start)
  (send! (keyword task))
  (mount/stop)
  (System/exit 0))


(comment
  (map #(select-keys % [:id :email :phone
                        :reminded_careseeker
                        :reminded_provider])
       (get-imminent-appointments))

  ;; reset all reminders
  (db/execute!
    ["UPDATE appointments
     SET reminded_careseeker = false,
         reminded_provider = false"])

  (doseq [appt (butlast (get-imminent-appointments))]
    (record-reminder! appt))

  (db/execute!
    ["UPDATE appointments
     SET reminded_careseeker = true,
         reminded_provider = true
     WHERE id = ?"
     60])

  (db/query
     (sql/format
      {:select [:appt.id :appt.start_time
                :appt.email :appt.phone :appt.ok_to_text
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
