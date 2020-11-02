(ns rtc.appointments.core
  (:require
    [clojure.set :refer [rename-keys]]
    [rtc.appointments.appointments :as appt]
    [rtc.appointments.availabilities :as avail]
    [rtc.appointments.states :as st]
    [rtc.appointments.windows :as w]))


;; TODO DELETE
(defn book-appointment-resolver [_context args _value]
  (println "BOOK" args))


;; Define our window length to be half an hour
(defonce WINDOW-MS (* 30 60 1000))


(defn appt-req->windows [{:keys [from to state] :as params}]
  (let [states (get st/state-mappings state)
        avails (avail/get-availabilities {:from from :to to :states states})
        appts (appt/get-appointments {:from from :to to :states states})]
    (map w/format-window
         (w/->windows (map w/coerce avails) (map w/coerce appts) from to WINDOW-MS))))

(comment

  (def now (java.util.Date.))
  (w/->windows [] [] (inst-ms now) (+ 360000000 (inst-ms now)) WINDOW-MS)
  (appt-req->windows {:from (inst-ms now) :to (+ (inst-ms now) (* 12 7 24 60 60 1000)) :state "WA"})

  )