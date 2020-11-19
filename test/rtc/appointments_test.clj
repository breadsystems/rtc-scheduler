(ns rtc.appointments-test
  (:require
   [clj-time.coerce :as c]
   [clojure.test :refer [deftest is]]
   [rtc.appointments.appointments :as appt]))



(deftest test-params->query
  ;; No need to join when not querying by state
  (is (nil? (:join (appt/params->query {}))))
  ;; Join on provider_id
  (is (= [[:users :p] [:and [:= :p.id :a.provider_id] [:= :p.is_provider true]]]
         (:join (appt/params->query {:states #{"CA"}}))))
  (is (= [:and [:= 1 1] [:in :p.state #{"NY"}]]
         (:where (appt/params->query {:states #{"NY"}}))))
  (is (= [:and
          [:= 1 1]
          [:in :p.state #{"CA"}]
          [:between
           :start_time
           (c/to-sql-time "2020-01-01T10:00:00.000-08:00")
           (c/to-sql-time "2020-01-07T10:00:00.000-08:00")]]
         (:where (appt/params->query {:states #{"CA"}
                                       :from   "2020-01-01T10:00:00.000-08:00"
                                       :to     "2020-01-07T10:00:00.000-08:00"})))))