(ns rtc.appointments.core
  (:require
   [rtc.appointments.appointments :as appt]
   [rtc.appointments.availabilities :as avail]
   [rtc.appointments.states :as st]
   [rtc.appointments.windows :as w]
   [rtc.util :as util]))


(defonce ONE-DAY-MS (* 24 60 60 1000))

;; Define our window length to be half an hour
(defonce WINDOW-MS (* 30 60 1000))

(defn- window-range []
  (let [;; TODO tighten up this logic for more accurate availability
        ;; Look for availabilities starting this time five days from now
        from (+ (inst-ms (util/midnight-this-morning)) (* 5 ONE-DAY-MS))
        to (+ from (* 28 ONE-DAY-MS))]
    [from to]))

(defn params->windows [{:keys [from to state]}]
  (let [states (get st/state-mappings state)
        avails (avail/get-availabilities {:from from :to to :states states})
        appts (appt/get-appointments {:from from :to to :states states})]
    (map w/format-window
         (w/->windows (map w/coerce avails) (map w/coerce appts) from to WINDOW-MS))))

(defn get-available-windows [params]
  (let [[from to] (window-range)
        state (get params "state")]
    (params->windows {:from from :to to :state state})))

(defn book-appointment! [{:keys [start end state] :as appt}]
  (let [[from to] (window-range)]
    {:success true
     :data "TODO"}))

(comment

  (def now (java.util.Date.))
  (w/->windows [] [] (inst-ms now) (+ 360000000 (inst-ms now)) WINDOW-MS)
  (appt-req->windows {:from (inst-ms now) :to (+ (inst-ms now) (* 12 7 24 60 60 1000)) :state "WA"}))