;; Availabilities.
;; This is where existing appointments and calendar availabilities
;; get turned into open appointment windows to present to careseekers.
(ns rtc.appointments.avail
  (:require
   [clj-time.coerce :as c]
   [honeysql.core :as sql]
   [java-time :as t]
   [rtc.db :as d]))


(defn create!
  "TODO"
  [])

(defn window-resolver [{:keys [from to state]}]
  [{:start_time ""
    :end_time ""
    :provider (d/get-provider {:id 5})}])

(defn params->query
  "Takes a map of params and returns a HoneySQL query map"
  [{:keys [from to state]}]
  {:select [:*]
   :from [[:availabilities :a]]
   :join
   (when state [[:providers :p] [:= :p.id :a.provider_id]])
   :where
   (filter some? [:and
                  (when state [:= :state state])
                  (when (and from to) [:between :start_time (c/to-sql-time from) (c/to-sql-time to)])])})

(defn get-availabilities [params]
  (-> params params->query sql/format d/query))


(comment
  (d/bind!)

  (t/sql-timestamp (t/local-date "yyyy-MM-dd" "2020-01-01"))

  (sql/format {:select [:*]
               :from [:appointments]
               :where [:and [:between :start_time "2020-01-01" "2020-01-07"] [:= :provider_id 123]]})
  ;; => ["SELECT * FROM appointments WHERE (start_time BETWEEN ? AND ? AND provider_id = ?)"
  ;;     "2020-01-01"
  ;;     "2020-01-07"
  ;;     123]

  ;; NOTE:
  ;; `AND` collapses with only a single clause:
  (sql/format {:select [:*]
               :from [:appointments]
               :where [:and [:= 1 1]]})
  ;; => ["SELECT * FROM appointments WHERE (? = ?)" 1 1]


  (d/query (sql/format {:select [:*]
                        :from [:availabilities]
                        :where [:and
                                [:between
                                 :start_time
                                 (c/to-sql-time #inst "2020-01-01T00:00:00.000-08:00")
                                 (c/to-sql-time #inst "2020-12-31T23:59:59.000-08:00")]
                                [:= :provider_id 1]]}))

  (get-availabilities {:from "2020-08-12" :to "2020-08-31" :state "WA"})
  (get-availabilities {:from "2020-08-12" :to "2020-08-31" :state "OR"})

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
                          :careseeker-id 2
                          :provider-id 1})

  (d/get-appointment {:id 1})
  (d/get-appointment {:id 5})

  (d/get-provider {:id 1})

  (d/create-availability! {:start (c/to-sql-time #inst "2020-08-11T11:30:00.000-08:00")
                           :end   (c/to-sql-time #inst "2020-08-11T17:00:00.000-08:00")
                           :provider-id 1})


  (d/get-availabilities {:join  (d/join-provider-availability)
                         :where (d/where-available
                                 {:cond
                                  [(d/available-between
                                    {:conj ""
                                     :start (c/to-sql-time #inst "2020-08-11T09:00:00.000-08:00")
                                     :end   (c/to-sql-time #inst "2020-08-13T17:00:00.000-08:00")})
                                   (d/available-in-state
                                    {:conj "AND"
                                     :state "OR"})]})})

  (d/create-appointment-need! {:need-id 1 :appointment-id 1 :info "Mandarin"})
  (d/get-appointment-need {:appointment-id 1})
  (d/delete-appointment-need! {:need-id 1 :appointment-id 1}))