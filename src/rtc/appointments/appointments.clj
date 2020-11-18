(ns rtc.appointments.appointments
  (:require
    [clj-time.coerce :as c]
    [honeysql.helpers :as sqlh]
    [honeysql.core :as sql]
    [rtc.db :as d])
  (:import
    [java.util Date]))


(defn create! [appt]
  (-> (sqlh/insert-into :appointments)
      (sqlh/values [appt])
      (sql/format)
      (d/execute!)))

(comment
  (def provider (d/query ["SELECT * FROM providers LIMIT 1"]))

  (def now (Date.))
  (create! {:start_time (c/to-sql-time (+ (inst-ms now) (* 24 60 60 1000)))
            :end_time (c/to-sql-time (+ (inst-ms now) (* 24 60 60 1000) (* 30 60 1000)))
            :name "Zoey"
            :email "zoey@dyke4prez.blue"
            :alias ""
            :pronouns "she/her"
            :ok_to_text true
            :date_created (c/to-sql-time now)
            :other_needs "I shall require forty-five green M&Ms"
            :provider_id (:id provider)}))

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

  (d/get-appointment {:id 1})
  (d/get-appointment {:id 5})

  (d/create-appointment-need! {:need-id 1 :appointment-id 1 :info "Mandarin"})
  (d/get-appointment-need {:appointment-id 1})
  (d/delete-appointment-need! {:need-id 1 :appointment-id 1})
  (get-appointments {:from (Date.) :to (Date.) :states #{"WA"}})

  (get-appointments {}))