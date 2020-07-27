(ns rtc.calendar-test
  (:require
   [clojure.test :refer [deftest is]]
   [rtc.admin.calendar :as cal]))


(deftest test-overlap-any?
  (let [availabilities [;; Aug 1, midnight - noon
                        {:start #inst "2020-08-01T00:00:00-00:00"
                         :end   #inst "2020-08-01T12:00:00-00:00"}
                        ;; Aug 2, 9am - 5pm
                        {:start #inst "2020-08-02T09:00:00-00:00"
                         :end   #inst "2020-08-02T17:00:00-00:00"}
                        ;; Aug 3, noon - 11:30
                        {:start #inst "2020-08-03T12:00:00-00:00"
                         :end   #inst "2020-08-03T23:30:00-00:00"}]]
    ;; Aug 1, 1am - 2am OVERLAPS ✔
    (is (cal/overlaps-any? {:start #inst "2020-08-01T01:00:00-00:00"
                            :end   #inst "2020-08-01T02:00:00-00:00"}
                           availabilities))
    ;; Aug 1, 9am - 2pm OVERLAPS ✔
    (is (cal/overlaps-any? {:start #inst "2020-08-01T09:00:00-00:00"
                            :end   #inst "2020-08-01T14:00:00-00:00"}
                           availabilities))
    ;; Aug 1, noon - 2pm DOES NOT OVERLAP ✗
    (is (false? (cal/overlaps-any? {:start #inst "2020-08-01T12:00:00-00:00"
                                    :end   #inst "2020-08-01T14:00:00-00:00"}
                                   availabilities)))
    ;; Aug 2, 8am - 6pm OVERLAPS ✔
    (is (cal/overlaps-any? {:start #inst "2020-08-02T08:00:00-00:00"
                            :end   #inst "2020-08-02T18:00:00-00:00"}
                           availabilities))
    ;; Aug 4, 8am - 6pm DOES NOT OVERLAP ✗
    (is (false? (cal/overlaps-any? {:start #inst "2020-08-04T08:00:00-00:00"
                                    :end   #inst "2020-08-04T18:00:00-00:00"}
                                   availabilities)))))

(deftest test-filter-by-provider
  (let [availabilities [{:start #inst "2020-08-02T09:00:00-00:00"
                         :end   #inst "2020-08-02T17:00:00-00:00"
                         :user/id 3}
                        {:start #inst "2020-08-02T09:00:00-00:00"
                         :end   #inst "2020-08-02T17:00:00-00:00"
                         :user/id 2}
                        {:start #inst "2020-08-03T09:00:00-00:00"
                         :end   #inst "2020-08-03T17:00:00-00:00"
                         :user/id 3}]]
    (is (= 2 (count (cal/filter-by-id availabilities 3))))))