;; Availabilities.
;; This is where existing appointments and calendar availabilities
;; get turned into open appointment windows to present to careseekers.
(ns rtc.appointments.avail
  (:require
   [clj-time.coerce :as c]
   [honeysql.core :as sql]
   [java-time :as t]
   [rtc.db :as d])
  (:import
    [java.util Date]))


(defn create!
  "TODO"
  [])

(defn window-resolver [{:keys [from to state]}]
  [{:start_time ""
    :end_time ""
    :provider (d/get-provider {:id 5})}])

(defn params->query
  "Takes a map of params and returns a HoneySQL query map"
  [{:keys [from to states]}]
  {:select [:*]
   :from   [[:availabilities :a]]
   :join
           (when states [[:providers :p] [:= :p.id :a.provider_id]])
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

  (d/get-provider {:id 1})

  (def one-hour (* 60 60 1000))
  (def six-hours (* 6 60 60 1000))
  (def one-day (* 24 60 60 1000))
  (def one-week (* 7 24 60 60 1000))
  (def now (Date.))

  (d/create-availability! {:start (c/to-sql-time now)
                           :end   (c/to-sql-time (+ (inst-ms now) six-hours))
                           :provider-id 1})
  (d/create-availability! {:start       (c/to-sql-time (+ (inst-ms now) one-day))
                           :end         (c/to-sql-time (+ (inst-ms now) one-day six-hours))
                           :provider-id 1})
  (d/create-availability! {:start       (c/to-sql-time (+ (inst-ms now) (* 2 one-day)))
                           :end         (c/to-sql-time (+ (inst-ms now) (* 2 one-day) six-hours))
                           :provider-id 1})

  (d/create-availability! {:start       (c/to-sql-time (+ one-week (inst-ms now)))
                           :end         (c/to-sql-time (+ one-week (inst-ms now) six-hours))
                           :provider-id 1})
  (d/create-availability! {:start       (c/to-sql-time (+ one-week (inst-ms now) one-day))
                           :end         (c/to-sql-time (+ one-week (inst-ms now) one-day six-hours))
                           :provider-id 1})
  (d/create-availability! {:start       (c/to-sql-time (+ one-week (inst-ms now) (* 2 one-day)))
                           :end         (c/to-sql-time (+ one-week (inst-ms now) (* 2 one-day) six-hours))
                           :provider-id 1})

  (get-availabilities {})
  (get-availabilities {:states #{"WA"}})
  (get-availabilities {:from (+ (inst-ms now) one-day) :to (+ (inst-ms now) (* 2 one-day)) :states #{"WA"}})
  (get-availabilities {:from "2020-08-12" :to "2020-08-31" :states #{"CA"}})
  (get-availabilities {:from "2020-08-12" :to "2020-08-31" :state #{"OR"}}))