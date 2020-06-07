(ns rtc.intake-core-test
  (:require
   [cljs.test :refer [deftest is]]
   [re-frame.core :as rf]
   [rtc.intake.core :as intake]))


(deftest current-questions-honors-current-step
  (let [db {:step 2
            :steps [{:name :zero
                     :questions []}
                    {:name :one
                     :questions [:whatevs]}
                    {:name :two
                     :questions [{:key :question2.0}
                                 {:key :question2.1}]}]}]
    (is (= [{:key :question2.0}
            {:key :question2.1}]
           (intake/current-questions db)))))

(deftest current-step-honors-step
  (let [db {:step 2
            :steps [{:name :zero} {:name :one} {:name :two}]}]
    (is (= {:name :two} (intake/current-step db)))))

(deftest accessible-routes
  (let [db {:step 2
            :viewed-up-to-step 3
            :steps [{:name :zero} {:name :one} {:name :two}]}
        routes [["/zero"  {:name :zero  :step 0}]
                ["/one"   {:name :one   :step 1}]
                ["/two"   {:name :two   :step 2}]
                ["/three" {:name :three :step 3}]
                ["/four"  {:name :four  :step 4}]]]
    (is (= [{:name :zero  :step 0 :current? false :viewed? true}
            {:name :one   :step 1 :current? false :viewed? true}
            {:name :two   :step 2 :current? true  :viewed? true}
            {:name :three :step 3 :current? false :viewed? true}
            {:name :four  :step 4 :current? false :viewed? false}]
           (intake/accessible-routes db routes)))))

(deftest answer-gets-corresponding-answer-by-key
  (let [db {:answers {:two+two "Four"}}]
    (is (= "Four" (intake/answer db [:_ :two+two])))
    (is (= "" (intake/answer db [:_ :one+one])))))


(deftest update-route
  (let [db {:step 0}]
    (is (= 42
           (:step (intake/update-route
                   db
                   [::intake/update-route {:name :whatever
                                           :step 42}]))))))

(deftest update-answer
  (let [db {:answers {:two+two ""}}]
    (is (= "Four"
           (get-in (intake/update-answer db [:_ :two+two "Four"])
                   [:answers :two+two])))))

(deftest next-and-previous-steps-work
  (let [db {:steps [1 2 3 4 5]
            :step 3}]
    (is (= 0 (:step (intake/prev-step (intake/prev-step (intake/prev-step (intake/prev-step db)))))))
    (is (= 0 (:step (intake/prev-step (intake/prev-step (intake/prev-step db))))))
    (is (= 1 (:step (intake/prev-step (intake/prev-step db)))))
    (is (= 2 (:step (intake/prev-step db))))
    (is (= 3 (:step (intake/next-step (intake/prev-step db)))))
    (is (= 4 (:step (intake/next-step db))))
    (is (= 5 (:step (intake/next-step (intake/next-step db)))))
    (is (= 5 (:step (intake/next-step (intake/next-step (intake/next-step db))))))))