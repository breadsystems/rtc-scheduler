(ns rtc.appointments-windows-test
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [clojure.test :refer [deftest is testing]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as prop]
   [rtc.appointments.windows :as w]))


;; (defspec drange-is-always-positive
;;   (prop/for-all
;;    [drange (spec/gen ::w/drange)]
;;    (let [[from to] drange]
;;      (> (inst-ms to) (inst-ms from)))))

;; (availabilities->windows avails appts from to w)
;; should not return more than P * (avails' / w) windows

;; Windows are maps whose keys are valid dranges

;; Given a window length w, the duration of each key's start/end 
;; insts should be EXACTLY w.

;; The earliest start time should be no earlier than `from`

;; The latest end time should be no later than `to`

;; All ids from every op are represented once ops are applied

(deftest test-->ids
  (is (= [1 2 4]
         (w/->ids {1 1, 2 1, 3 0, 4 1, 5 -1}))))

(deftest test-avail->ops

  ;; Whereas we want to be more liberal with appointments -
  ;; i.e. if they say they'll take up half a window, assume they'll
  ;; take up the full window - we want to be more conservative
  ;; with availabilities - i.e. if a rad doc says they're available
  ;; for forty minutes, round that down to 30. Crucially, don't assume
  ;; that extra ten minutes rolls over into the next 30-minute window.

  (testing "when start/end are both at window edges"
    (is (= [[0 3 1] [50 3 -1]]
           (w/avail->ops 0 100 50 {:start 0 :end 50 :id 3})))
    (is (= [[50 3 1] [100 3 -1]]
           (w/avail->ops 0 100 50 {:start 50 :end 100 :id 3}))))

  (testing "when start/end are off a window edge"
    ;; Spanning less than a single window; effectively a noop.
    (is (= [[50 3 1] [0 3 -1]] (w/avail->ops 0 100 50 {:start 5 :end 45 :id 3})))
    ;; Spanning two windows, but not available for any single full window,
    ;; which causes start/end to collapse down to a single edge, which is
    ;; effectively a noop.
    (is (= [[50 3 1] [50 3 -1]] (w/avail->ops 0 100 50 {:start 5 :end 95 :id 3})))
    ;; Finally, (partially) spanning three windows, a 5 - 105 availability
    ;; collapses down to the 50 - 100 window.
    (is (= [[50 3 1] [100 3 -1]] (w/avail->ops 0 150 50 {:start 5 :end 105 :id 3}))))

  (testing "when availability starts one or more windows before from"
    ;; Start gets normalized to from; we don't care about windows
    ;; before that point. 
    (is (= [[50 3 1] [500 3 -1]]
           (w/avail->ops 50 600 50 {:start 0 :end 500 :id 3})))
    (is (= [[100 3 1] [500 3 -1]]
           (w/avail->ops 100 600 50 {:start 0 :end 500 :id 3})))
    (is (= [[300 3 1] [500 3 -1]]
           (w/avail->ops 300 600 50 {:start 0 :end 500 :id 3})))
    (is (= [[300 3 1] [500 3 -1]]
           (w/avail->ops 300 600 50 {:start 100 :end 500 :id 3}))))

  (testing "when availability ends one or more windows after to"
    (is (= [[300 3 1] [500 3 -1]]
           (w/avail->ops 300 500 50 {:start 0 :end 550 :id 3})))
    (is (= [[300 3 1] [500 3 -1]]
           (w/avail->ops 300 500 50 {:start 100 :end 600 :id 3})))
    (is (= [[300 3 1] [500 3 -1]]
           (w/avail->ops 300 500 50 {:start 100 :end 1000 :id 3}))))

  (testing "with irregular window edges"
    ;; Start/end should get normalized to 1 and 51 resp.
    (is (= [[1 3 1] [51 3 -1]]
           (w/avail->ops 1 101 50 {:start 0 :end 100 :id 3})))
    ;; Because start is after from,
    ;; Start should now get normalized to 51, effectively a noop.
    (is (= [[51 3 1] [51 3 -1]]
           (w/avail->ops 1 101 50 {:start 2 :end 100 :id 3})))
    ;; Similar deal, but spanning multiple windows.
    (is (= [[51 3 1] [151 3 -1]]
           (w/avail->ops 1 201 50 {:start 7 :end 165 :id 3}))))

  (testing "with an irregular window length"
    ;; 7 gets bumped UP to window edge 13;
    ;; 11 get bumped DOWN to 0, effectively resulting in a noop.
    (is (= [[13 3 1] [0 3 -1]] (w/avail->ops 0 39 13 {:start 7 :end 11 :id 3})))
    ;; 17 gets bumped UP to window edge 26 (the next highest
    ;; multiple of 13); 27 get bumped DOWN to 26 (the next
    ;; lowest multiple of 13), effectively resulting in a noop.
    (is (= [[26 3 1] [26 3 -1]]
           (w/avail->ops 0 52 13 {:start 17 :end 27 :id 3})))
    ;; 11 gets bumped UP to window edge 13,
    ;; 42 gets bumped DOWN to 39,
    ;; and finally we have a conservative two windows of
    ;; availability.
    (is (= [[13 3 1] [39 3 -1]]
           (w/avail->ops 0 52 13 {:start 11 :end 42 :id 3})))))

(deftest test-appt->ops
  
  (testing "when start is at a window edge"
    (is (= [[0 3 -1] [50 3 1]]
           (w/appt->ops 0 100 50 {:start 0 :end 50 :id 3})))
    (is (= [[50 3 -1] [100 3 1]]
           (w/appt->ops 0 100 50 {:start 50 :end 100 :id 3}))))
  
  (testing "when start/end are off a window edge"
    ;; Spanning a single window
    (is (= [[0 3 -1] [50 3 1]]
           (w/appt->ops 0 100 50 {:start 5 :end 45 :id 3})))
    ;; Spanning two windows
    (is (= [[0 3 -1] [100 3 1]]
           (w/appt->ops 0 100 50 {:start 5 :end 95 :id 3}))))

  (testing "with irregular window edges"
    ;; Start/end should get "normalized" to 1, 99 resp.
    (is (= [[1 3 -1] [99 3 1]]
           (w/appt->ops 1 99 50 {:start 5 :end 90 :id 3})))
    ;; Similar deal, but spanning multiple windows.
    (is (= [[1 3 -1] [151 3 1]]
           (w/appt->ops 1 201 50 {:start 7 :end 142 :id 3}))))
  
  (testing "with an irregular window length"
    ;; 7 gets bumped down to window edge 0,
    ;; 11 get bumped up to 13
    (is (= [[0 3 -1] [13 3 1]]
           (w/appt->ops 0 39 13 {:start 7 :end 11 :id 3})))
    ;; 17 gets bumped down to window edge 13,
    ;; 27 get bumped up to 39 (the next highest multiple of 13)
    (is (= [[13 3 -1] [39 3 1]]
           (w/appt->ops 0 52 13 {:start 17 :end 27 :id 3})))))

(deftest test-apply-ops

  (let [initial {1 0, 2 0, 3 0}]
    (testing "with zero ops"
      ;; Literal noop 😃
      (is (= initial (w/apply-ops initial []))))

    (testing "with a previously non-existent id"
      (is (= {42 1}
             (w/apply-ops {} [[:_ 42 1]])))
      (is (= {1 0, 42 1}
             (w/apply-ops {1 0} [[:_ 42 1]])))
      (is (= {1 0, 42 1, 628 -1}
             (w/apply-ops {1 0} [[:_ 42 1] [:_ 628 -1]])))
      (is (= {1 0, 42 0, 628 -1}
             (w/apply-ops {1 0} [[:_ 42 1] [:_ 628 -1] [:_ 42 -1]]))))
    
    (testing "with one or more ops"
      ;; Just one availability for id 1
      (is (= {1 1, 2 0, 3 0}
             (w/apply-ops initial [[:_ 1 1]])))
      ;; availability for id 1, and two ops on id 2 that cancel each other out
      (is (= {1 1, 2 0, 3 0}
             (w/apply-ops initial [[:_ 1 1] [:_ 2 -1] [:_ 2 1]])))
      ;; canceling avail/appt, and then another availability for 2
      (is (= {1 1, 2 1, 3 0}
             (w/apply-ops initial [[:_ 1 1] [:_ 2 -1] [:_ 2 1] [:_ 2 1]])))
      ;; availabilities for each id
      (is (= {1 1, 2 1, 3 1}
             (w/apply-ops initial [[:_ 1 1] [:_ 2 1] [:_ 3 1]])))
      ;; availability for 1, but an appointment much later in the chain
      (is (= {1 0, 2 1, 3 1}
             (w/apply-ops initial [[:_ 1 1] [:_ 2 1] [:_ 3 1] [:_ 1 -1]]))))))

(deftest test-fold-ops
  
  (let [windows {0 [] 1 [] 2 [] 3 [] 4 []}]
    (testing "with zero ops"
      (is (= windows (reduce w/fold-ops windows []))))
    
    (testing "with one or more ops"
      (is (= {0 [[0 42 -1]]
              1 []
              2 []
              3 []
              4 []}
             (reduce w/fold-ops windows [[0 42 -1]])))
      (is (= {0 [[0 42 -1] [0 35 1]]
              1 []
              2 []
              3 []
              4 []}
             (reduce w/fold-ops windows [[0 42 -1] [0 35 1]])))
      (is (= {0 [[0 42 -1]]
              1 [[1 35 1]]
              2 []
              3 []
              4 []}
             (reduce w/fold-ops windows [[0 42 -1] [1 35 1]])))
      (is (= {0 [[0 42 -1]]
              1 [[1 35 1] [1 35 -1]]
              2 []
              3 []
              4 []}
             (reduce w/fold-ops windows [[0 42 -1] [1 35 1] [1 35 -1]])))
      (is (= {0 [[0 42 -1]]
              1 [[1 35 1] [1 35 -1]]
              2 [[2 10 1]]
              3 [[3 10 -1]]
              4 [[4 4 1]]}
             (reduce w/fold-ops windows [[4 4 1] [0 42 -1] [1 35 1] [2 10 1] [3 10 -1] [1 35 -1]]))))))

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

    (testing "overlapping with the begining of an availabilty window"
      (is (= [{:start 100 :end 150 :ids [2]}
              {:start 150 :end 200 :ids [2]}
              {:start 200 :end 250 :ids [2]}
              {:start 250 :end 300 :ids [2]}]
             ;; Simulate availability query results; availabilities
             ;; after this will normally not be returned from the db.
             (w/->windows (take 1 avails) [] 0 300 50))))

    (testing "overlapping with the tail end of an availabilty window"
      (is (= [{:start 300 :end 350 :ids [2]}
              {:start 350 :end 400 :ids [2]}
              {:start 400 :end 450 :ids [2]}
              {:start 450 :end 500 :ids [2]}]
             ;; Simulate availability query results.
             (w/->windows (take 1 avails) [] 300 600 50))))

    (testing "surrounding an entire availabilty window"
      (is (= [{:start 100 :end 150 :ids [2]}
              {:start 150 :end 200 :ids [2]}
              {:start 200 :end 250 :ids [2]}
              {:start 250 :end 300 :ids [2]}
              {:start 300 :end 350 :ids [2]}
              {:start 350 :end 400 :ids [2]}
              {:start 400 :end 450 :ids [2]}
              {:start 450 :end 500 :ids [2]}]
             ;; Simulate availability query results.
             (w/->windows (take 1 avails) [] 0 750 50))))

    (testing "with appointments"
      (is (= [{:start 100 :end 150 :ids [2]}
              {:start 150 :end 200 :ids [2]}
              {:start 250 :end 300 :ids [2]}
              {:start 300 :end 350 :ids [2]}
              {:start 350 :end 400 :ids [2]}]
             ;; Simulate availability query results.
             (w/->windows (take 1 avails) [{:start 200 :end 250 :id 2}] 0 400 50)))
      (is (= [{:start 150 :end 200 :ids [2]}
              {:start 250 :end 300 :ids [2]}
              {:start 300 :end 350 :ids [2]}
              {:start 350 :end 400 :ids [2]}]
             ;; Simulate availability query results.
             (w/->windows (take 1 avails) [{:start 100 :end 150 :id 2}
                                           {:start 200 :end 250 :id 2}] 0 400 50)))
      ;; all booked up
      (is (= []
             ;; Simulate availability query results.
             (w/->windows (take 1 avails)
                          [{:start 100 :end 150 :id 2}
                           {:start 150 :end 200 :id 2}
                           {:start 200 :end 250 :id 2}
                           {:start 250 :end 300 :id 2}
                           {:start 300 :end 350 :id 2}
                           {:start 350 :end 400 :id 2}]
                          0 400 50)))
      ;; one long-ass appointmenttttt
      (is (= [] (w/->windows (take 1 avails) [{:start 100 :end 500 :id 2}] 0 400 50))))

    (testing "with appointments for other doctors"
      (is (= [{:start 100 :end 150 :ids [2]}
              {:start 150 :end 200 :ids [2]}
              {:start 200 :end 250 :ids [2]}
              {:start 250 :end 300 :ids [2]}
              {:start 300 :end 350 :ids [2]}
              {:start 350 :end 400 :ids [2]}
              {:start 400 :end 450 :ids [2]}
              {:start 450 :end 500 :ids [2]}]
             ;; Simulate availability query results.
             (w/->windows (take 1 avails) [{:start 100 :end 150 :id 1}] 0 600 50)))
      (is (= [{:start 100 :end 150 :ids [2]}
              {:start 150 :end 200 :ids [2]}
              {:start 200 :end 250 :ids [2]}
              {:start 250 :end 300 :ids [2]}
              {:start 300 :end 350 :ids [2]}
              {:start 350 :end 400 :ids [2]}
              {:start 400 :end 450 :ids [2]}
              {:start 450 :end 500 :ids [2]}]
             ;; Simulate availability query results.
             (w/->windows (take 1 avails) [{:start 100 :end 150 :id 1}
                                           {:start 150 :end 200 :id 3}
                                           {:start 250 :end 700 :id 42}] 0 600 50))))

    (testing "when duration is less than `w`"
      (is (thrown? java.lang.AssertionError
                   (w/->windows avails [] 0 10 50))))

    (testing "when overlap is less than `w`"
      ;; before any availabilities begin
      (is (= []
             (w/->windows avails [] 0 50 50)))
      ;; just on the cusp of the first availability
      (is (= []
             (w/->windows avails [] 0 100 50)))
      ;; just shy of the *end* of the first availability
      (is (= []
             (w/->windows avails [] 0 149 50)))
      (is (= []
             (w/->windows avails [] 50 149 50)))
      ;; starting just before the end of the first window
      (is (= []
             (w/->windows avails [] 451 501 50)))
      (is (= []
             (w/->windows avails [] 499 599 50)))
      ;; starting just as the first availability ends
      (is (= []
             (w/->windows avails [] 500 1000 50))))))


(comment
  

  
  ;;
  )