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


(comment
  

  
  ;;
  )