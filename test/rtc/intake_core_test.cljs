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
  (let [cofx {:db {:steps [0 1 2 3 4]
                   :viewed-up-to-step 3
                   :step 3}}]
    ;; The main thing next/prev-step do is update the currect :step in the db.
    (is (= 0 (:step (:db (intake/prev-step (intake/prev-step (intake/prev-step (intake/prev-step cofx))))))))
    (is (= 0 (:step (:db (intake/prev-step (intake/prev-step (intake/prev-step cofx)))))))
    (is (= 1 (:step (:db (intake/prev-step (intake/prev-step cofx))))))
    (is (= 2 (:step (:db (intake/prev-step cofx)))))
    (is (= 3 (:step (:db (intake/next-step (intake/prev-step cofx))))))
    (is (= 4 (:step (:db (intake/next-step cofx)))))
    (is (= 4 (:step (:db (intake/next-step (intake/next-step cofx))))))

    ;; We also keep track of the max step the user has navigated to, so they can jump
    ;; forward to a step they've already reached if they want to.
    (is (= 3 (:viewed-up-to-step (:db (intake/prev-step (intake/prev-step (intake/prev-step cofx)))))))
    (is (= 3 (:viewed-up-to-step (:db (intake/prev-step (intake/prev-step cofx))))))
    (is (= 3 (:viewed-up-to-step (:db (intake/prev-step cofx)))))
    (is (= 3 (:viewed-up-to-step (:db (intake/next-step (intake/prev-step cofx))))))
    (is (= 4 (:viewed-up-to-step (:db (intake/next-step cofx)))))
    (is (= 4 (:viewed-up-to-step (:db (intake/next-step (intake/next-step cofx))))))

    ;; Finally, we also dispatch a ::location, effect to update the current URL.
    (is (= 0 (::intake/location (intake/prev-step (intake/prev-step (intake/prev-step cofx))))))
    (is (= 1 (::intake/location (intake/prev-step (intake/prev-step cofx)))))
    (is (= 2 (::intake/location (intake/prev-step cofx))))
    (is (= 3 (::intake/location (intake/next-step (intake/prev-step cofx)))))
    (is (= 4 (::intake/location (intake/next-step cofx))))
    (is (= 4 (::intake/location (intake/next-step (intake/next-step cofx)))))))

(deftest can-go-prev-limits-back-button
  (is (false? (intake/can-go-prev? {:step 0})))
  (is (false? (intake/can-go-prev? {:step -1})))
  (is (true?  (intake/can-go-prev? {:step 1})))
  (is (true?  (intake/can-go-prev? {:step 2})))
  (is (true?  (intake/can-go-prev? {:step 3}))))

(deftest can-go-next-limits-next-button
  (is (true?  (intake/can-go-next? {:step -1 :steps [0 1 2 3]})))
  (is (true?  (intake/can-go-next? {:step 0 :steps [0 1 2 3]})))
  (is (true?  (intake/can-go-next? {:step 1 :steps [0 1 2 3]})))
  (is (true?  (intake/can-go-next? {:step 2 :steps [0 1 2 3]})))
  (is (false? (intake/can-go-next? {:step 3 :steps [0 1 2 3]})))
  (is (false? (intake/can-go-next? {:step 4 :steps [0 1 2 3]})))
  
  (let [incomplete {:step 2
                    :answers {:required-a "A"
                              :required-b "B"
                              :required-blank ""}
                    :steps [{:questions [{:key :required-a :required? true}]}
                            {:questions [{:key :required-b :required? true}]}
                            {:questions [{:key :required-blank :required? true}
                                         {:key :optional-c}]}
                            {:questions [{:key :optional-d}]}]}]
    (is (false? (intake/can-go-next? incomplete)))))

(deftest question->validator-generates-correct-validator
  (let [validator (intake/question->validator {:key :optional-field})]
    (is (= [] (validator {})))
    (is (= [] (validator {:optional-field ""})))
    (is (= [] (validator {:optional-field "whatever"}))))
  (let [validator (intake/question->validator {:key :required-field :required? true})]
    (is (= [] (validator {:required-field "not blank"})))
    (is (= [{:for :required-field :message [:please-enter :required-field]}] (validator {})))
    (is (= [{:for :required-field :message [:please-enter :required-field]}] (validator {:required-field ""}))))

  ;; Test that custom error messages override default required message
  (let [validator (intake/question->validator {:key :required-field
                                               :required? true
                                               :required-message [:custom-error-message]})]
    (is (= [] (validator {:required-field "not blank"})))
    (is (= [{:for :required-field :message [:custom-error-message]}] (validator {:required-field ""}))))

  ;; Test requiring one field if the other is blank
  (let [validator (intake/question->validator {:key :thing-one
                                               :required-without-any? [:thing-two]})]
    (is (= [] (validator {:thing-one "not blank" :thing-two ""})))
    (is (= [] (validator {:thing-one "" :thing-two "not blank"})))
    (is (= [{:for :thing-one :message [:please-enter :thing-one]}] (validator {:thing-one "" :thing-two ""}))))
  (let [validator (intake/question->validator {:key :thing-two
                                               :required-message [:custom]
                                               :required-without-any? [:thing-one]})]
    (is (= [] (validator {:thing-one "not blank" :thing-two ""})))
    (is (= [] (validator {:thing-one "" :thing-two "not blank"})))
    (is (= [{:for :thing-two :message [:custom]}] (validator {:thing-one "" :thing-two ""})))))

(deftest display-error-for-correctly-considers-touched-fields
  (let [db {:touched #{:a :c :e}}]
    (is (true?  (intake/display-errors-for? db :a)))
    (is (false? (intake/display-errors-for? db :b)))
    (is (true?  (intake/display-errors-for? db :c)))
    (is (false? (intake/display-errors-for? db :d)))
    (is (true?  (intake/display-errors-for? db :e)))))

(deftest touch-field-correctly-adds-field-key-to-touched
  (let [db {:touched #{:a :b}}]
    (is (= #{:a :b}    (:touched (intake/touch db [:_ :a]))))
    (is (= #{:a :b :c} (:touched (intake/touch db [:_ :c]))))))

(deftest errors-for-considers-touched
  (let [db {:answers {:a "" :b "Bee" :c "" :d ""}
            :touched #{:a :b :c}
            :step 1
            :steps [{:questions [{:key :whatevs}]}
                    {:questions [{:key :a :required? true}
                                 {:key :b :required? true}
                                 {:key :c}
                                 {:key :d :required? true}]}]}]
    ;; :a is touched, required, and empty
    (is (= [{:for :a :message [:please-enter :a]}] (intake/errors-for db [:_ :a])))
    ;; :b is touched, required, and not empty
    (is (= [] (intake/errors-for db [:_ :b])))
    ;; :c is touched, but not required
    (is (= [] (intake/errors-for db [:_ :c])))
    ;; :d is required, but untouched
    (is (= [] (intake/errors-for db [:_ :d])))))