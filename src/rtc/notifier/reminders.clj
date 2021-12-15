(ns rtc.notifier.reminders
  (:require
    ;; HoneySQL 2.x
    [honey.sql :as sql]
    [rtc.db :as db]
    [rtc.env :refer [env]]
    [rtc.notifier.appointments :as appt]
    [mount.core :refer [defstate]]))

(defmulti send! identity)



(defmethod send! :appointments [_]
  (prn 'appointments!!!1))

(defn -main [task & _]
  (send! (keyword task)))


(comment
  (db/get-imminent-appointments)
  ;; SELECT * FROM appointments
  ;; WHERE now() > start_time - interval '24 hours' AND now() < start_time
  ;; AND (reminded_provider = false OR reminded_careseeker = false)

  (sql/format-expr [:date_add [:now] [:interval 24 "hours"]])
  (db/query
     (sql/format
      {:select [:*]
       :from [[:appointments :appt]]
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
