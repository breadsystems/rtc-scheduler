(ns rtc.admin.schedule
  (:require
   [clojure.set :refer [rename-keys]]
   [rtc.appointments.appointments :as appt]
   [rtc.appointments.availabilities :as avail]
   [rtc.users.core :as u]
   [rtc.util :refer [index-by]])
  (:import
   [java.util Date]))

(rename-keys {:x :y} {:x :x/x})
(defonce one-week (* 7 24 60 60 1000))

(defn- one-week-ago []
  (- (inst-ms (Date.)) one-week))

(defn- one-week-from-now []
  (+ (inst-ms (Date.)) one-week))

(defn schedule [{:keys [identity query-params]}]
  (let [appts (appt/get-appointments
               {:from (one-week-ago)
                :to (* 8 (one-week-from-now))})
        avails (avail/get-availabilities
                {:from (one-week-ago)
                 :to (* 8 (one-week-from-now))})
        rename-keys* #(rename-keys % {:provider_id :user/id
                                      :start_time :start
                                      :end_time :end})
        assoc-access-needs #(assoc % :access-needs (appt/id->needs (:id %)))]
    {:appointments
     (as-> appts $
       (map (comp assoc-access-needs rename-keys*) $)
       (index-by :id $))
     :availabilities
     (as-> avails $
       (map rename-keys* $)
       (index-by :id $))
     :user-id (:id identity)
     :users
     (index-by :id (u/all))
     :params query-params}))


(comment

  ;; TODO exceptions, y tho
  (appt/get-appointments {})
  (avail/get-availabilities {})

  ;;
  )