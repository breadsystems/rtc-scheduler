;; Availabilities.
;; This is where existing appointments and calendar availabilities
;; get turned into open appointment windows to present to careseekers.
(ns rtc.appointments.availabilities
  (:require
   [clj-time.coerce :as c]
   [clojure.spec.alpha :as spec]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [java-time :as t]
   [rtc.db :as d]
   [rtc.users.core :as u]
   [rtc.util :refer [six-hours one-day one-week]])
  (:import
    [java.util Date]))


(spec/def ::start_time inst?)
(spec/def ::end_time inst?)
(spec/def ::provider_id int?)

(spec/def ::availability (spec/and
                          (spec/keys :req-un [::start_time ::end_time ::provider_id])
                          #(< (inst-ms (:start_time %)) (inst-ms (:end_time %)))))


(defn get-overlapping [{:keys [start_time end_time provider_id]}]
  (let [from (c/to-sql-time start_time)
        to (c/to-sql-time end_time)]
    (d/query
     (sql/format
      {:select [:*]
       :from [[:availabilities :a]]
       :where [:and
               [:= :a.provider_id provider_id]
               [:or
                [:between :a.start_time from to]
                [:and [:<= from :a.end_time] [:>= from :a.start_time]]
                [:between :a.end_time from to]]]}))))

(defn create!
  "Given a provider_id and a time range, ({:start_time x :end_time y}), create an availility."
  [{:keys [start_time end_time provider_id] :as avail}]
  {:pre [(spec/valid? ::availability avail)]}
  (-> (sqlh/insert-into :availabilities)
      (sqlh/values [{:start_time (c/to-sql-time start_time)
                     :end_time (c/to-sql-time end_time)
                     :provider_id provider_id}])
      (sql/format)
      (d/execute!))
  ;; TODO improve this?
  (let [{:keys [id provider_id start_time end_time]}
        (first (d/query ["SELECT * FROM availabilities ORDER BY id DESC LIMIT 1"]))]
    {:id id :user/id provider_id :start start_time :end end_time}))

(defn update!
  "Update an availability in the database.
   Does not support updating provider_id."
  [{:keys [id start_time end_time provider_id] :as avail}]
  {:pre [(int? id) (spec/valid? ::availability avail)]}
  (-> (sqlh/update :availabilities)
      (sqlh/sset {:start_time (c/to-sql-time start_time)
                  :end_time (c/to-sql-time end_time)})
      (sqlh/where [:= :id id])
      (sql/format)
      (d/execute!))
  {:id id :user/id provider_id :start start_time :end end_time})

(defn delete!
  "Given an availability ID, deletes the availability from the database."
  [id]
  {:pre [(int? id)]}
  (-> {:delete-from :availabilities :where [:= :id id]}
      (sql/format)
      (d/execute!)))

(comment

  (spec/valid? ::availability {:start_time #inst "2020"
                               :end_time #inst "2021"
                               :provider_id 123})
  ;; => true

  ;; Hopefully by 2050 this code will be obsolete because we'll
  ;; have public  healthcare in this shithole country.
  (spec/explain-data ::availability {:start_time #inst "2050"
                                     :end_time (Date.)
                                     :provider_id 123})

  (spec/explain-data ::availability {:start_time #inst "2020"
                                     :end_time #inst "2021"
                                     :provider_id 'invalid!!lol})

  (spec/explain-data ::availability {})

  (create! {}) ;; fail

  (create! {:start_time #inst "2020-11-07T10:00:00-08:00"
            :end_time #inst "2020-11-07T17:00:00-08:00"
            :provider_id 456}))

(defn params->query
  "Takes a map of params and returns a HoneySQL query map"
  [{:keys [from to states]}]
  {:select [:*]
   :from   [[:availabilities :a]]
   :join
           (when states [[:users :p] [:and [:= :p.id :a.provider_id] [:= :p.is_provider true]]])
   :where
           (filter some? [:and
                          [:= 1 1]
                          (when states [:in :p.state states])
                          (when (and from to) [:between :start_time (c/to-sql-time from) (c/to-sql-time to)])])})

(defn get-availabilities [params]
  (-> params params->query sql/format d/query))


(comment
  (d/bind!)

  (t/sql-timestamp (t/local-date "yyyy-MM-dd" "2020-01-01"))

  (d/query (sql/format {:select [:*] :from [:careseekers]}))
  (d/query (sql/format {:select [:*] :from [:availabilities]}))
  (d/query (sql/format {:select [:*] :from [:appointments]}))

  (def now (Date.))

  (get-availabilities {})
  (get-availabilities {:states #{"WA"}})
  (get-availabilities {:from (+ (inst-ms now) one-day) :to (+ (inst-ms now) (* 2 one-day)) :states #{"WA"}})
  (get-availabilities {:from "2020-08-12" :to "2020-08-31" :states #{"CA"}})
  (get-availabilities {:from "2020-08-12" :to "2020-08-31" :state #{"OR"}}))