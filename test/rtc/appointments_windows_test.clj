(ns rtc.appointments-windows-test
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as prop]
   [rtc.appointments.windows :as w]))


;; (availabilities->windows avails appts from to w)
;; should not return more than P * (avails'/w) windows

#_(defspec total-windows=appointments+available-windows 10
  (prop/for-all
   [total-vs-appts (spec/gen (spec/and
                              (spec/coll-of pos-int? :kind vector? :count 2)
                              (fn [[total appts]]
                                (>= total appts))))]
   (let [[total appt-count] total-vs-appts]
     appt-count)))

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

  ;;  
  )