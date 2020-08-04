(ns rtc.appointments-windows-test
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [clojure.test :refer [deftest is testing]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as prop]
   [rtc.appointments.windows :as w]))


(defspec drange-is-always-positive
  (prop/for-all
   [drange (spec/gen ::w/drange)]
   (let [[from to] drange]
     (> (inst-ms to) (inst-ms from)))))

;; (availabilities->windows avails appts from to w)
;; should not return more than P * (avails' / w) windows

;; Windows are maps whose keys are valid dranges

;; Given a window length w, the duration of each key's start/end 
;; insts should be EXACTLY w.

;; The earliest start time should be no earlier than `from`

;; The latest end time should be no later than `to`

(deftest test-->windows
  ;; This tests a simplified model of time using nice round numbers
  ;; for readability. But the concept is exactly the same when applied
  ;; to timestamps, which are just integers.
  ;; In this simplified timeline:
  ;; * hours are 100 minutes
  ;; * days are 1000 minutes (10 hours)
  ;; * the standard window length is 50
  (let [;; Imaginary provider ids:
        ;; * 1 - Shevek
        ;; * 2 - Takver
        ;; * 3 - Genly Ai
        avails [;; DAY 0: only Takver
                ;; 1-5 Takver
                {:start 100 :end 500 :id 2}

                ;; DAY 1: No availabilities.

                ;; DAY 2: Shevek & Takver available w/ some overlap
                ;; 2-4 Shevek
                {:start 2200 :end 2500 :id 1}
                ;; 3-6 Takver
                {:start 2300 :end 2600 :id 2}

                ;; DAY 3: Genly Ai available for two distinct timeframes
                ;; 3-5 Genly
                {:start 3300 :end 3500 :id 3}
                ;; 7-9
                {:start 3300 :end 3500 :id 3}]]

;;     (testing "with some overlap at start"
;;       (is (= [{:start 100 :end 150 :provider-ids [2]}
;;               {:start 150 :end 200 :provider-ids [2]}
;;               {:start 200 :end 250 :provider-ids [2]}
;;               {:start 250 :end 300 :provider-ids [2]}]
;;              (w/->windows avails [] 0 300 50))))

    (testing "when `to` is before the first availability"
      (is (= []
             (w/->windows avails [] -1000 -500 50)))
      (is (= []
             (w/->windows avails [] -100 0 50)))
      (is (= []
             (w/->windows avails [] 0 50 50)))
      (is (= []
             (w/->windows avails [] 49 99 50))))

    (testing "when `from` is after the last availability"
      (is (= []
             (w/->windows avails [] 3500 4000 50)))
      (is (= []
             (w/->windows avails [] 10000 20000 50))))

    (testing "when duration is less than `w`"
      (is (thrown? java.lang.AssertionError
                   (w/->windows avails [] 0 10 50))))

    (testing "when overlap is less than `w`"
      (is (= []
             (w/->windows avails [] 0 149 50)))
      (is (= []
             (w/->windows avails [] 451 1000 50))))
    ;;
    ))

(deftest test-availabilities->windows
  (let [;; Imaginary provider ids:
        ;; * 1 - Shevek
        ;; * 2 - Takver
        ;; * 3 - Genly Ai
        avails [;; Jan 1
                ;; Takver - 10a-2p
                {:start #inst "2020-01-01T10:00"
                 :end   #inst "2020-01-01T14:00"
                 :provider-id 2}

                ;; Jan 2 - No availabilities

                ;; Jan 3 - Shevek & Takver available w/ some overlap
                ;; Shevek - 11a-2p
                {:start #inst "2020-01-03T11:00"
                 :end   #inst "2020-01-03T14:00"
                 :provider-id 1}
                ;; Takver - 12:30p-3p
                {:start #inst "2020-01-03T12:30"
                 :end   #inst "2020-01-03T15:00"
                 :provider-id 2}

                ;; Jan 4 - no overlaps, only one provider available
                ;; for two distinct timeframes
                ;; Genly Ai - 10a-11:30a
                {:start #inst "2020-01-04T10:00"
                 :end   #inst "2020-01-04T11:30"
                 :provider-id 3}
                ;; Genly Ai - 3p-4:30p
                {:start #inst "2020-01-04T15:00"
                 :end   #inst "2020-01-04T16:30"
                 :provider-id 3}]
        ->30m-windows (fn [appts start end]
                        (->> (w/availabilities->windows avails appts start end 30)
                             (map #(select-keys % [:start :end :provider-ids]))))]

    (testing "when `to` is before the first availability"
      (is (= []
             (->30m-windows [] #inst "2000-01-01" #inst "2001-01-02")))
      (is (= []
             (->30m-windows [] #inst "2019-01-01" #inst "2019-01-31")))
      (is (= []
             (->30m-windows [] #inst "2019-12-01" #inst "2019-12-31"))))

    (testing "when `from` is after the last availability"
      (is (= []
             (->30m-windows [] #inst "2020-01-07T18:00" #inst "2020-01-07T20:00")))
      (is (= []
             (->30m-windows [] #inst "2020-01-02" #inst "2020-01-03")))
      (is (= []
             (->30m-windows [] #inst "2020-02-01" #inst "2020-02-28")))
      (is (= []
             (->30m-windows [] #inst "2020-02-01" #inst "2020-12-31"))))

    (testing "when overlap is less than w"
      (is (= []
             (->30m-windows [] #inst "2020-01-01T00:00" #inst "2020-01-01T10:29")))
      (is (= []
             (->30m-windows [] #inst "2020-01-01T10:00" #inst "2020-01-01T10:01")))
      ;; Just after last window would start
      (is (= []
             (->30m-windows [] #inst "2020-01-07T17:31" #inst "2020-01-07T17:59")))
      (is (= []
             (->30m-windows [] #inst "2020-01-07T17:31" #inst "2020-01-07T18:31")))
      ;; Just before last availability ends
      (is (= []
             (->30m-windows [] #inst "2020-01-07T17:59" #inst "2020-01-07T18:31"))))

    (testing "with a one-hour overlap"
      (is (= [;; Takver's first two available slots on Jan 1
              {:start #inst "2020-01-01T10:00"
               :end   #inst "2020-01-01T10:30"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T10:30"
               :end   #inst "2020-01-01T11:00"
               :provider-ids #{2}}]
             (->30m-windows [] #inst "2020-01-01T10:00" #inst "2020-01-01T11:00"))))

    (testing "around a full availability"
      (is (= [;; All of Takver's slots on Jan 1
              {:start #inst "2020-01-01T10:00"
               :end   #inst "2020-01-01T10:30"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T10:30"
               :end   #inst "2020-01-01T11:00"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T11:00"
               :end   #inst "2020-01-01T11:30"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T11:30"
               :end   #inst "2020-01-01T12:00"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T12:00"
               :end   #inst "2020-01-01T12:30"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T12:30"
               :end   #inst "2020-01-01T13:00"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T13:00"
               :end   #inst "2020-01-01T13:30"
               :provider-ids #{2}}
              {:start #inst "2020-01-01T13:30"
               :end   #inst "2020-01-01T14:00"
               :provider-ids #{2}}]
             (->30m-windows [] #inst "2020-01-01T09:00" #inst "2020-01-01T15:00"))))

    (testing "in the middle with no availabilies"
      (is (= []
             (->30m-windows [] #inst "2020-01-02T00:00" #inst "2020-01-02T23:59"))))

    (testing "around multiple docs with overlapping availability"
      (is (= [;; Shevek 11a-2p & Takver 12:30p-3p.
              ;; First, only Shevek is available in the morning...
              {:start #inst "2020-01-03T11:00"
               :end   #inst "2020-01-03T11:30"
               :provider-ids #{1}}
              {:start #inst "2020-01-03T11:30"
               :end   #inst "2020-01-03T12:00"
               :provider-ids #{1}}
              {:start #inst "2020-01-03T12:00"
               :end   #inst "2020-01-03T12:30"
               :provider-ids #{1}}
              ;; ...then Takver becomes available too
              {:start #inst "2020-01-03T12:30"
               :end   #inst "2020-01-03T13:00"
               :provider-ids #{1 2}}
              {:start #inst "2020-01-03T13:00"
               :end   #inst "2020-01-03T13:30"
               :provider-ids #{1 2}}
              {:start #inst "2020-01-03T13:30"
               :end   #inst "2020-01-03T14:00"
               :provider-ids #{1 2}}
              ;; ...now Shevek is no longer available
              {:start #inst "2020-01-03T14:00"
               :end   #inst "2020-01-03T14:30"
               :provider-ids #{2}}
              {:start #inst "2020-01-03T14:30"
               :end   #inst "2020-01-03T15:00"
               :provider-ids #{2}}]
             (->30m-windows [] #inst "2020-01-03T08:00" #inst "2020-01-03T22:00"))))

    (testing "around separate availability windows for the same doc"
      (is (= [;; Genly Ai - 10a-11:30a & 3p-4:30p
              {:start #inst "2020-01-04T10:00"
               :end   #inst "2020-01-04T10:30"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T10:30"
               :end   #inst "2020-01-04T11:00"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T11:00"
               :end   #inst "2020-01-04T11:30"
               :provider-ids #{3}}
              ;; start of second availability window
              {:start #inst "2020-01-04T15:00"
               :end   #inst "2020-01-04T15:30"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T15:30"
               :end   #inst "2020-01-04T16:00"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T16:00"
               :end   #inst "2020-01-04T16:30"
               :provider-ids #{3}}]
             (->30m-windows [] #inst "2020-01-04T08:00" #inst "2020-01-04T20:00"))))

    #_(testing "with appointments for a different doc"
      (is (= [;; Genly Ai - 10a-11:30a & 3p-4:30p
              {:start #inst "2020-01-04T10:00"
               :end   #inst "2020-01-04T10:30"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T10:30"
               :end   #inst "2020-01-04T11:00"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T11:00"
               :end   #inst "2020-01-04T11:30"
               :provider-ids #{3}}
              ;; start of second availability window
              {:start #inst "2020-01-04T15:00"
               :end   #inst "2020-01-04T15:30"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T15:30"
               :end   #inst "2020-01-04T16:00"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T16:00"
               :end   #inst "2020-01-04T16:30"
               :provider-ids #{3}}]
             (->30m-windows [{:start #inst "2020-01-04T15:00"
                              :end   #inst "2020-01-04T15:30"
                              :provider-id 1}
                             {:start #inst "2020-01-04T15:30"
                              :end   #inst "2020-01-04T16:00"
                              :provider-id 1}]
                            #inst "2020-01-04T08:00" #inst "2020-01-04T20:00"))))

    #_(testing "with appointments for the same doc"
      (is (= [;; Genly Ai - 10a-11:30a & 3p-4:30p
              {:start #inst "2020-01-04T10:00"
               :end   #inst "2020-01-04T10:30"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T10:30"
               :end   #inst "2020-01-04T11:00"
               :provider-ids #{3}}
              {:start #inst "2020-01-04T11:00"
               :end   #inst "2020-01-04T11:30"
               :provider-ids #{3}}
              ;; start of second availability window, minus appts
              {:start #inst "2020-01-04T16:00"
               :end   #inst "2020-01-04T16:30"
               :provider-ids #{3}}]
             (->30m-windows [{:start #inst "2020-01-04T15:00"
                              :end   #inst "2020-01-04T15:30"
                              :provider-id 3}
                             {:start #inst "2020-01-04T15:30"
                              :end   #inst "2020-01-04T16:00"
                              :provider-id 3}]
                            #inst "2020-01-04T08:00" #inst "2020-01-04T20:00"))))))

(comment
  (gen/sample (spec/gen (spec/int-in 0 0)))

  (drop 100 (gen/sample (spec/gen ::w/date) 110))

  (drop 50 (gen/sample (spec/gen ::w/appointment-count) 60))
  (gen/sample (spec/gen (spec/int-in 1 50)))
  ;; => (2 2 1 2 8 2 3 8 22 32)
  (drop 100 (gen/sample (spec/gen ::w/availability) 110))
  (gen/sample (spec/gen ::w/window-minutes))
  (gen/sample (spec/gen ::w/provider-id))

  (gen/vector (spec/gen ::w/date))
  (drop 20 (gen/sample (spec/gen ::w/window-length) 30))
  ;; => (9 847 4466 4 2 6149714 6 45 2 673)

  (gen/sample (spec/gen ::w/drange))

  (gen/sample (spec/gen ::w/availability))
  (gen/sample (spec/gen ::w/appointment))

  ;;  
  )