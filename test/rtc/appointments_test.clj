(ns rtc.appointments-test
  (:require
   [clj-time.coerce :as c]
   [clojure.test :refer [deftest is]]
   [rtc.appointments.avail :as avail]))



(deftest test-params->query
  ;; No need to join when not querying by state
  (is (nil? (:join (avail/params->query {}))))
  ;; Join on provider_id
  (is (= [[:providers :p] [:= :p.id :a.provider_id]]
         (:join (avail/params->query {:state "CA"}))))
  (is (= [:and [:= :state "NY"]]
         (:where (avail/params->query {:state "NY"}))))
  (is (= [:and
          [:= :state "CA"]
          [:between
           :start_time
           (c/to-sql-time "2020-01-01T10:00:00.000-08:00")
           (c/to-sql-time "2020-01-07T10:00:00.000-08:00")]]
         (:where (avail/params->query {:state "CA"
                                       :from "2020-01-01T10:00:00.000-08:00"
                                       :to "2020-01-07T10:00:00.000-08:00"})))))