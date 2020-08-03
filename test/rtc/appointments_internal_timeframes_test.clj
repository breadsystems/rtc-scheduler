;; Test complex internal logic used in the availability window algorithm.
(ns rtc.appointments-internal-timeframes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [rtc.appointments.internal.timeframes :as t]))


(deftest test-inst-min
  (is (= #inst "2019-12-31"
         (t/inst-min #inst "2019-12-31" #inst "2020-01-01"))))

(deftest test-inst-max
  (is (= #inst "2020-01-01"
         (t/inst-max #inst "2019-01-31" #inst "2020-01-01"))))

(deftest test-before?
  (is (false? (t/before? #inst "2020-02-01" #inst "2020-01-01")))
  (is (true? (t/before? #inst "2020-01-01" #inst "2020-02-01"))))

(deftest test-after?
  (is (true? (t/after? #inst "2020-02-01" #inst "2020-01-01")))
  (is (false? (t/after? #inst "2020-01-01" #inst "2020-02-01"))))

(deftest test-plus
  (is (= #inst "2020-01-01T00:00:01" (t/plus #inst "2020-01-01T00:00:00" 1000)))
  (is (= #inst "2020-01-01T00:00:10" (t/plus #inst "2020-01-01T00:00:00" (* 10 1000))))
  (is (= #inst "2020-01-01T00:01:00" (t/plus #inst "2020-01-01T00:00:00" (* 60 1000))))
  (is (= #inst "2020-01-01T01:00:00" (t/plus #inst "2020-01-01T00:00:00" (* 60 60 1000)))))

(deftest test-minus
  (is (= #inst "2020-01-01T00:00:00" (t/minus #inst "2020-01-01T00:00:01" 1000)))
  (is (= #inst "2020-01-01T00:00:00" (t/minus #inst "2020-01-01T00:00:10" (* 10 1000))))
  (is (= #inst "2020-01-01T00:00:00" (t/minus #inst "2020-01-01T00:01:00" (* 60 1000))))
  (is (= #inst "2020-01-01T00:00:00" (t/minus #inst "2020-01-01T01:00:00" (* 60 60 1000)))))

(deftest test-avail->timeframe

  (testing "with no overlap"
    (let [avail {:start #inst "2020-01-01T09:00"
                 :end   #inst "2020-01-01T12:00"}]
      (is (nil? (t/avail->timeframe avail #inst "2020-01-02" #inst "2020-01-03")))))

  (let [avail {:start #inst "2020-01-01T09:00"
               :end   #inst "2020-01-01T14:00"
               :provider-id 1}]
    (testing "with overlap at the start"
      ;; Request overlaps with our availability window 9-noon
      (is (= {:start #inst "2020-01-01T09:00"
              :end   #inst "2020-01-01T12:00"
              :provider-id 1}
             (t/avail->timeframe avail #inst "2020-01-01T00:00" #inst "2020-01-01T12:00"))))

    (testing "with overlap at the end"
      ;; Request overlaps with our availability window 12-2
      (is (= {:start #inst "2020-01-01T12:00"
              :end   #inst "2020-01-01T14:00"
              :provider-id 1}
             (t/avail->timeframe avail #inst "2020-01-01T12:00" #inst "2020-01-01T15:00"))))

    (testing "when from/to surrounds start/end"
      ;; Request surrounds the entire availability window
      (is (= {:start #inst "2020-01-01T09:00"
              :end   #inst "2020-01-01T14:00"
              :provider-id 1}
             (t/avail->timeframe avail #inst "2020-01-01T08:00" #inst "2020-01-01T15:00"))))

    (testing "when start/end surrounds from/to"
      (is (= {:start #inst "2020-01-01T12:00"
              :end   #inst "2020-01-01T13:30"
              :provider-id 1}
             (t/avail->timeframe avail #inst "2020-01-01T12:00" #inst "2020-01-01T13:30"))))))

(deftest test-timeframe->window-map

  (testing "into 30-minute windows"
    (is (= {[#inst "2020-01-01T12:00" #inst "2020-01-01T12:30"] 1
            [#inst "2020-01-01T12:30" #inst "2020-01-01T13:00"] 1
            [#inst "2020-01-01T13:00" #inst "2020-01-01T13:30"] 1
            [#inst "2020-01-01T13:30" #inst "2020-01-01T14:00"] 1
            [#inst "2020-01-01T14:00" #inst "2020-01-01T14:30"] 1}
           (t/timeframe->window-map {:start #inst "2020-01-01T12:00"
                                     :end   #inst "2020-01-01T14:30"
                                     :provider-id 1}
                                    30))))

  (testing "into 60-minute windows"
    (is (= {[#inst "2020-01-01T12:00" #inst "2020-01-01T13:00"] 1
            [#inst "2020-01-01T13:00" #inst "2020-01-01T14:00"] 1}
           (t/timeframe->window-map {:start #inst "2020-01-01T12:00"
                                     :end   #inst "2020-01-01T14:30"
                                     :provider-id 1}
                                    60))))

  (testing "into 2-hour windows"
    (is (= {[#inst "2020-01-01T12:00" #inst "2020-01-01T14:00"] 1}
           (t/timeframe->window-map {:start #inst "2020-01-01T12:00"
                                     :end   #inst "2020-01-01T14:30"
                                     :provider-id 1}
                                    120))))

  (testing "into 3-hour windows"
    (is (= {} ;; Should be empty since no 3-hour window completely overlaps
           (t/timeframe->window-map {:start #inst "2020-01-01T12:00"
                                     :end   #inst "2020-01-01T14:30"
                                     :provider-id 1}
                                    180)))))

#_(deftest test-earliest-window-edge
  (testing "with a start time long after from"
    (is (= #inst "2020-01-01T10:00"
           (t/earliest-window-edge #inst "2020-01-01T10:00"
                                   #inst "2020-01-01T06:00"
                                   30))))

  (testing "with a start time shortly after from"
    (is (= #inst "2020-01-01T10:00"
           (t/earliest-window-edge #inst "2020-01-01T10:00"
                                   #inst "2020-01-01T09:30"
                                   30))))

  (testing "with an irregular start time long after from"
    (is (= #inst "2020-01-01T10:00"
           (t/earliest-window-edge #inst "2020-01-01T10:00"
                                   #inst "2020-01-01T06:00"
                                   30))))

  (testing "with a start time equal to from"
    (is (= #inst "2020-01-01T10:00"
           (t/earliest-window-edge #inst "2020-01-01T10:00"
                                   #inst "2020-01-01T10:00"
                                   30))))
  )

#_(deftest test-appt->windows
  (testing "with partially overlapping appointment"
    (is (= [[#inst "2020-01-01T11:30" #inst "2020-01-01T12:00"]
            [#inst "2020-01-01T12:00" #inst "2020-01-01T12:30"]]
           (t/appt->windows {:start #inst "2020-01-01T11:45"
                             :end   #inst "2020-01-01T12:15"}
                            #inst "2020-01-01T11:30"
                            30)))

    (is (= [[#inst "2020-01-01T11:30" #inst "2020-01-01T12:00"]
            [#inst "2020-01-01T12:00" #inst "2020-01-01T12:30"]]
           (t/appt->windows {:start #inst "2020-01-01T11:45"
                             :end   #inst "2020-01-01T12:15"}
                            #inst "2020-01-01T00:00"
                            30))))

  (testing "with irregular from time"
    (is (= [[#inst "2020-01-01T11:33" #inst "2020-01-01T12:03"]
            [#inst "2020-01-01T12:03" #inst "2020-01-01T12:33"]]
           (t/appt->windows {:start #inst "2020-01-01T11:45"
                             :end   #inst "2020-01-01T12:15"}
                            #inst "2020-01-01T11:33"
                            30))))

  (testing "with a 20-minute window length"
    (is (= [[#inst "2020-01-01T11:40" #inst "2020-01-01T12:00"]
            [#inst "2020-01-01T12:00" #inst "2020-01-01T12:20"]
            [#inst "2020-01-01T12:20" #inst "2020-01-01T12:40"]]
           (t/appt->windows {:start #inst "2020-01-01T11:45"
                             :end   #inst "2020-01-01T12:30"}
                            #inst "2020-01-01T11:40"
                            20)))))