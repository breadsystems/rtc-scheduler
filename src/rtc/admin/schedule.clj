(ns rtc.admin.schedule
  (:require
   [rtc.appointments.appointments :as appt]
   [rtc.appointments.availabilities :as avail]
   [rtc.db :as d]
   [rtc.util :refer [index-by]])
  (:import
   [java.util Date]))

(defonce one-week (* 7 24 60 60 1000))

(defn- one-week-ago []
  (- (inst-ms (Date.)) one-week))

(defn- one-week-from-now []
  (+ (inst-ms (Date.)) one-week))

(defn schedule [{:keys [query-params] :as req}]
  {:appointments
   (index-by :id (appt/get-appointments
                  {:from (one-week-ago)
                   :to (* 8 (one-week-from-now))}))
   :availabilities
   (index-by :id (avail/get-availabilities
                  {:from (one-week-ago)
                   :to (* 8 (one-week-from-now))}))
   :users
   (index-by :id (d/get-all-providers))
   :params query-params})


(comment

  ;; TODO exceptions, y tho
  (appt/get-appointments {})
  (avail/get-availabilities {})

  ;;
  )