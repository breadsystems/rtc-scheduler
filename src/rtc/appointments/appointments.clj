(ns rtc.appointments.appointments
  (:require
   [clojure.spec.alpha :as spec]
   [clj-time.coerce :as c]
   [honeysql.helpers :as sqlh]
   [honeysql.core :as sql]
   [rtc.db :as d]
   [rtc.providers.core :as p])
  (:import
    [java.util Date]))


(spec/def ::id int?)
(spec/def ::start_time inst?)
(spec/def ::end_time inst?)
(spec/def ::provider_id int?)
(spec/def ::reason string?)

(spec/def ::appointment
  (spec/and
   (spec/keys :req-un [::start_time ::end_time ::provider_id ::reason]
              :opt-un [::id])
   #(> (inst-ms (:end_time %)) (inst-ms (:start_time %)))))


(defn create! [appt]
  {:pre [(spec/valid? ::appointment appt)]}
  (-> (sqlh/insert-into :appointments)
      (sqlh/values [appt])
      (sql/format)
      (d/execute!)))

(comment
  (def provider (p/email->provider "lauren@tamayo.email"))

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
           (when states [[:users :p] [:and [:= :p.id :a.provider_id] [:= :p.is_provider true]]])
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