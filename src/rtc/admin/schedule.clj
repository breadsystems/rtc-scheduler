(ns rtc.admin.schedule
  (:require
    [rtc.appointments.appointments :as appt]
    [rtc.appointments.avail :as avail]))


(defn schedule [_]
  ;; TODO parameterize from/to
  {:appointments   (appt/get-appointments {})
   :availabilities (avail/get-availabilities {})
   :users          []})


(comment

  ;; TODO exceptions, y tho
  (appt/get-appointments {})
  (avail/get-availabilities {})

  ;;
  )