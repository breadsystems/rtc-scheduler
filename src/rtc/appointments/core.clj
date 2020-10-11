(ns rtc.appointments.core
  (:require
    [rtc.appointments.windows :as w]
    [rtc.appointments.appointments :as appt]
    [rtc.appointments.avail :as avail]))


;; TODO DELETE
(defn book-appointment-resolver [_context args _value]
  (println "BOOK" args))


;; Define our window length to be half an hour
(defonce WINDOW-MS (* 30 60 1000))

(defn appt-req->windows [{:keys [from to state] :as params}]
  (let [avails (avail/get-availabilities {:from from :to to :state state})
        appts  (appt/get-appointments    {:from from :to to :state state})]
    {:params         params
     :availabilities avails
     :appointments   appts
     :windows        (w/->windows avails appts from to WINDOW-MS)}))


(comment

  (def now (java.util.Date.))
  (w/->windows [] [] (inst-ms now) (+ 360000000 (inst-ms now)) WINDOW-MS)

  )