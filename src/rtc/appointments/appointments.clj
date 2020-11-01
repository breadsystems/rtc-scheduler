(ns rtc.appointments.appointments
  (:require
    [clj-time.coerce :as c]
    [honeysql.core :as sql]
    [rtc.db :as d])
  (:import
    [java.util Date]))


(defn params->query
  "Takes a map of params and returns a HoneySQL query map"
  [{:keys [from to states]}]
  {:select [:*]
   :from [[:appointments :a]]
   :join
           (when states [[:providers :p] [:= :p.id :a.provider_id]])
   :where
           (filter some? [:and
                          [:= 1 1]
                          ;; TODO state mappings
                          (when states [:in :p.state states])
                          ;; TODO migrate away from clj-time
                          (when (and from to) [:between :start_time (c/to-sql-time from) (c/to-sql-time to)])])})

(defn get-appointments [params]
  (-> params params->query sql/format d/query))


(comment
  (c/to-sql-time (inst-ms (Date. 2021 01 01)))
  (d/create-need! {:name "Interpretation"
                   :description "Translation service for a non-English speaker"})
  (d/get-needs)
  (d/get-need {:id 1})

  (d/create-appointment! {:start (c/to-sql-time #inst "2020-08-09T10:30:00.000-08:00")
                          :end (c/to-sql-time #inst "2020-08-09T10:50:00.000-08:00")
                          :reason "I have questions about my HRT"
                          :careseeker-id 1
                          :provider-id 1})

  (d/create-appointment! {:start (c/to-sql-time #inst "2020-08-10T09:00:00.000-08:00")
                          :end (c/to-sql-time #inst "2020-08-10T09:20:00.000-08:00")
                          :reason "I have questions about my medication"
                          :careseeker-id 1
                          :provider-id 1})

  (d/get-appointment {:id 1})
  (d/get-appointment {:id 5})

  (d/create-appointment-need! {:need-id 1 :appointment-id 1 :info "Mandarin"})
  (d/get-appointment-need {:appointment-id 1})
  (d/delete-appointment-need! {:need-id 1 :appointment-id 1})
  (get-appointments {:from (Date.) :to (Date.) :states #{"WA"}})

  (get-appointments {}))