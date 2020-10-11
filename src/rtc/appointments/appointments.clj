(ns rtc.appointments.appointments
  (:require
    [clj-time.coerce :as c]
    [honeysql.core :as sql]
    [rtc.db :as d])
  (:import
    [java.util Date]))


(defn params->query
  "Takes a map of params and returns a HoneySQL query map"
  [{:keys [from to state]}]
  {:select [:*]
   :from [[:appointments :a]]
   :join
           (when state [[:providers :p] [:= :p.id :a.provider_id]])
   :where
           (filter some? [:and
                          ;; TODO state mappings
                          (when state [:= :p.state state])
                          ;; TODO migrate away from clj-time
                          (when (and from to) [:between :start_time (c/to-sql-time from) (c/to-sql-time to)])])})

(defn get-appointments [params]
  (-> params params->query sql/format d/query))


(comment
  (c/to-sql-time (inst-ms (Date. 2021 01 01)))
  (get-appointments {:from (Date.) :to (Date.) :state "WA"}))