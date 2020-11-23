(ns rtc.admin.schedule
  (:require
   [clojure.set :refer [rename-keys]]
   [rtc.appointments.appointments :as appt]
   [rtc.appointments.availabilities :as avail]
   [rtc.db :as d]
   [rtc.util :refer [index-by]])
  (:import
   [java.util Date]))

(rename-keys {:x :y} {:x :x/x})
(defonce one-week (* 7 24 60 60 1000))

(defn- one-week-ago []
  (- (inst-ms (Date.)) one-week))

(defn- one-week-from-now []
  (+ (inst-ms (Date.)) one-week))

(defn schedule [{:keys [query-params]}]
  (let [appts (appt/get-appointments
               {:from (one-week-ago)
                :to (* 8 (one-week-from-now))})
        avails (avail/get-availabilities
                {:from (one-week-ago)
                 :to (* 8 (one-week-from-now))})
        key-mapping {:provider_id :user/id
                     :start_time :start
                     :end_time :end}]
    {:appointments
     (as-> appts $
       (map #(rename-keys % key-mapping) $)
       (index-by :id $))
     :availabilities
     (as-> avails $
       (map #(rename-keys % key-mapping) $)
       (index-by :id $))
     :users
     (index-by :id (d/get-all-providers))
     :params query-params}))


(comment

  ;; TODO exceptions, y tho
  (appt/get-appointments {})
  (avail/get-availabilities {})

  ;;
  )