(ns rtc.calendar-test
  (:require
   [clojure.test :refer [deftest is testing]]
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

(deftest test-update-availability
  (let [db {:availabilities {1 {:id 1 :user/id 11}
                             2 {:id 2 :user/id 12}
                             3 {:id 3 :user/id 13}}}]
    (is (= {:id 3 :user/id 13 :start "new start date"}
           (get-in (cal/update-availability db [:_ 3 {:start "new start date"}])
                   [:availabilities 3])))))

(deftest test-delete-availability
  (let [db {:availabilities {1 {:id 1 :user/id 11}
                             2 {:id 2 :user/id 12}
                             3 {:id 3 :user/id 13}}}]
    (is (= [1 3]
           (keys (:availabilities (cal/delete-availability db [:_ "2"])))))))

(deftest test-appointment?
  (is (true? (cal/appointment? {:event/type :appointment})))
  (is (true? (cal/appointment? #js {:extendedProps #js {:type "appointment"}}))))

(deftest test-availability?
  (is (true? (cal/availability? {:event/type :availability})))
  (is (true? (cal/availability? #js {:extendedProps #js {:type "availability"}}))))

(deftest test-visible-events

  (let [db {:availabilities {1 {:id 1 :user/id 2}
                             2 {:id 2 :user/id 2}
                             3 {:id 3 :user/id 2}
                             4 {:id 4 :user/id 3}
                             5 {:id 5 :user/id 3}
                             6 {:id 6 :user/id 3}
                             7 {:id 7 :user/id 4}
                             8 {:id 8 :user/id 4}}
            :appointments {;; Octavia needs CC, so should show up
                           ;; when filtering by access need/CC.
                           1 {:id 1 :user/id 3 :name "Octavia"
                              :access-needs #{{:need/id 10}}}
                           ;; Usula needs Spanish interpretation, so should show up
                           ;; when filtering by access need/interpreter.
                           2 {:id 2 :user/id 2 :name "Ursula"
                              :access-needs #{{:need/id 11 :interpreter-lang "Khmer"}}}
                           ;; Anon needs Spanish interpretation, so should show up
                           ;; when filtering by access need/interpreter.
                           3 {:id 3 :user/id 4 :name "Anonymous"
                              :access-needs #{{:need/id 11 :interpreter-lang "Español"}}}
                           ;; Kim has no access needs to accommodate, so should not show up
                           ;; when filtering by access need.
                           4 {:id 4 :user/id 2 :name "Kim"
                              :access-needs #{}}
                           ;; Janelle needs a Somali interpreter, but that has been fulfilled
                           ;; by contact 14 (Weglot), so this should not show up when filtering
                           ;; by access need.
                           5 {:id 5 :user/id 2 :name "Janelle"
                              :access-needs #{{:need/id 11 :interpreter-lang "Somali"}}
                              :fulfillments #{{:need/id 11 :contact/id 14}}}}
            :users {2 {:id 2 :first_name "Lauren" :last_name "Olamina"}
                    3 {:id 3 :first_name "Shevek"}
                    4 {:id 4 :first_name "Genly" :last_name "Ai"}}
            :filters {:appointments? true
                      :availabilities? true
                      :providers #{2 3 4}
                      :access-needs #{}}
            :needs {10 {:id 10 :name "Closed Captioning"}
                    11 {:id 11 :name "Interpreter"}
                    12 {:id 12 :name "VRT"}}
            :contacts {13 {:id 13 :name "Language Link"}
                       14 {:id 14 :name "Weglot"}}
            :user-id 3
            :colors [:red :blue :green]}
        with-filters (fn [db filters]
                       (update db :filters merge filters))
        ;; Make tests more scannable!
        simplify (fn [events]
                   (map #(select-keys % [:id :event/type]) events))]

    (testing "when displaying all calendar events"
      (is (= [{:id 1 :event/type :availability}
              {:id 2 :event/type :availability}
              {:id 3 :event/type :availability}
              {:id 4 :event/type :availability}
              {:id 5 :event/type :availability}
              {:id 6 :event/type :availability}
              {:id 7 :event/type :availability}
              {:id 8 :event/type :availability}
              {:id 1 :event/type :appointment}
              {:id 2 :event/type :appointment}
              {:id 3 :event/type :appointment}
              {:id 4 :event/type :appointment}
              {:id 5 :event/type :appointment}]
             (simplify (cal/visible-events db)))))
    (testing "when filtering to availabilities only"
      (is (= [{:id 1 :event/type :availability}
              {:id 2 :event/type :availability}
              {:id 3 :event/type :availability}
              {:id 4 :event/type :availability}
              {:id 5 :event/type :availability}
              {:id 6 :event/type :availability}
              {:id 7 :event/type :availability}
              {:id 8 :event/type :availability}]
             (simplify (cal/visible-events (with-filters db {:appointments? false}))))))
    (testing "when filtering to appointments only"
      (is (= [{:id 1 :event/type :appointment}
              {:id 2 :event/type :appointment}
              {:id 3 :event/type :appointment}
              {:id 4 :event/type :appointment}
              {:id 5 :event/type :appointment}]
             (simplify (cal/visible-events (with-filters db {:availabilities? false}))))))
    (testing "when filtering by provider"
      (is (= [{:id 1 :event/type :availability}
              {:id 2 :event/type :availability}
              {:id 3 :event/type :availability}
              {:id 4 :event/type :availability}
              {:id 5 :event/type :availability}
              {:id 6 :event/type :availability}
              {:id 1 :event/type :appointment}
              {:id 2 :event/type :appointment}
              {:id 4 :event/type :appointment}
              {:id 5 :event/type :appointment}]
             (simplify (cal/visible-events (with-filters db {:providers #{2 3}}))))))
    (testing "when filtering by access need"
      (is (= [{:id 2 :event/type :appointment}
              {:id 3 :event/type :appointment}]
             ;; Filter by Needs Interpreter and Needs VRT
             (simplify (cal/visible-events (with-filters db {:access-needs #{11 12}
                                                             :availabilities? false})))))
      (is (= [{:id 1 :event/type :appointment}
              {:id 2 :event/type :appointment}
              {:id 3 :event/type :appointment}]
             ;; Filter by Needs CC and Needs Interpreter
             (simplify (cal/visible-events (with-filters db {:access-needs #{10 11}
                                                             :availabilities? false})))))
      (is (= [{:id 1 :event/type :appointment}]
             ;; Filter by Needs CC only
             (simplify (cal/visible-events (with-filters db {:access-needs #{10}
                                                             :availabilities? false})))))
      (is (= []
             ;; Filter by Needs VRT only
             (simplify (cal/visible-events (with-filters db {:access-needs #{12}
                                                             :availabilities? false})))))
      (is (= []
             ;; Filter by a non-existent access need
             (simplify (cal/visible-events (with-filters db {:access-needs #{42}
                                                             :availabilities? false})))))
      (is (= [{:id 1 :event/type :appointment}
              {:id 2 :event/type :appointment}
              {:id 3 :event/type :appointment}
              {:id 4 :event/type :appointment}
              {:id 5 :event/type :appointment}]
             ;; Clear access need filters
             (simplify (cal/visible-events (with-filters db {:access-needs #{}
                                                             :availabilities? false}))))))

    ;; {:availabilities {1 {:id 1 :user/id 2} Lauren Olamina
    ;;                   2 {:id 2 :user/id 2}
    ;;                   3 {:id 3 :user/id 2}
    ;;                   4 {:id 4 :user/id 3} Shevek
    ;;                   5 {:id 5 :user/id 3}
    ;;                   6 {:id 6 :user/id 3}
    ;;                   7 {:id 7 :user/id 4} Genly Ai
    ;;                   8 {:id 8 :user/id 4}}
    ;;  :appointments {1 {:id 1 :user/id 3 :name "Octavia"}
    ;;                 2 {:id 2 :user/id 2 :name "Ursula"}
    ;;                 3 {:id 3 :user/id 4 :name "Anonymous"}
    ;;                 4 {:id 4 :user/id 2 :name "Kim"}
    ;;                 5 {:id 5 :user/id 2 :name "Janelle"}}}
    (testing "Availabilities and Appointments display the correct title"
      (is (= [;; Availabilities display the providers's name
              "Lauren Olamina"
              "Lauren Olamina"
              "Lauren Olamina"
              "Shevek"
              "Shevek"
              "Shevek"
              "Genly Ai"
              "Genly Ai"
              ;; Appointments display the careseeker's name
              "Octavia"
              "Ursula"
              "Anonymous"
              "Kim"
              "Janelle"]
             (map :title (cal/visible-events db)))))

    (testing "the current user's availabilities are deletable for them"
      (is (= [false false false true true true false false nil nil nil nil nil]
             (map :deletable (cal/visible-events db)))))))

(deftest test-update-filter

  (let [db {:filters {:appointments? true
                      :availabilities? true
                      :providers #{1 2 3}
                      :access-needs #{4 5 6}}}]
    ;; Appointments/Availabilities filters are just normal, boring toggles.
    (is (false? (-> db
                    (cal/update-filter [:_ :appointments? nil])
                    (get-in [:filters :appointments?]))))
    (is (true? (-> db
                   (cal/update-filter [:_ :appointments? nil])
                   (cal/update-filter [:_ :appointments? nil])
                   (get-in [:filters :appointments?]))))
    (is (false? (-> db
                    (cal/update-filter [:_ :appointments? nil])
                    (cal/update-filter [:_ :appointments? nil])
                    (cal/update-filter [:_ :appointments? nil])
                    (get-in [:filters :appointments?]))))

    (is (false? (-> db
                    (cal/update-filter [:_ :availabilities? nil])
                    (get-in [:filters :availabilities?]))))
    (is (true? (-> db
                   (cal/update-filter [:_ :availabilities? nil])
                   (cal/update-filter [:_ :availabilities? nil])
                   (get-in [:filters :availabilities?]))))
    (is (false? (-> db
                    (cal/update-filter [:_ :availabilities? nil])
                    (cal/update-filter [:_ :availabilities? nil])
                    (cal/update-filter [:_ :availabilities? nil])
                    (get-in [:filters :availabilities?]))))

    ;; Updating a Provider filter toggles membership in the
    ;; set of selected providers.
    (is (= #{1 2}
           (-> db
               (cal/update-filter [:_ :providers 3])
               (get-in [:filters :providers]))))
    (is (= #{1 2 3}
           (-> db
               (cal/update-filter [:_ :providers 3])
               (cal/update-filter [:_ :providers 3])
               (get-in [:filters :providers]))))
    (is (= #{1 2 3 4}
           (-> db
               (cal/update-filter [:_ :providers 4])
               (get-in [:filters :providers]))))

    ;; Updating an Access Need filter toggles membership in the
    ;; set of selected needs.
    (is (= #{4 5}
           (-> db
               (cal/update-filter [:_ :access-needs 6])
               (get-in [:filters :access-needs]))))
    (is (= #{4 5 6}
           (-> db
               (cal/update-filter [:_ :access-needs 6])
               (cal/update-filter [:_ :access-needs 6])
               (get-in [:filters :access-needs]))))
    (is (= #{4 5 6 7}
           (-> db
               (cal/update-filter [:_ :access-needs 7])
               (get-in [:filters :access-needs]))))))

(deftest test-clear-filter
  
  (testing "it resets all selected filters to the empty set"
    (let [db {:filters {:access-needs #{1 2 3}}}]
      (is (= #{}
             (-> db
                 (cal/clear-filter [:_ :access-needs])
                 (get-in [:filters :access-needs])))))))

(deftest test-users-by-id
  (testing "it cycles through available colors"
    (let [db {:users {1 {:id 1}
                      2 {:id 2}
                      3 {:id 3}
                      4 {:id 4}
                      5 {:id 5}}
              :colors ["red" "blue" "green"]}] ;; RGB FTW
      (is (= [{:id 1 :color "red"}
              {:id 2 :color "blue"}
              {:id 3 :color "green"}
              {:id 4 :color "red"}
              {:id 5 :color "blue"}]
             (cal/providers db))))))

(deftest test-access-needs-filter-summary
  (let [db {:filters {:appointments? true
                      :access-needs #{}}
            :needs {1 {:id 1 :name "Love"}
                    2 {:id 2 :name "Fulfillment"}
                    3 {:id 3 :name "Food"}
                    4 {:id 4 :name "Shelter"}}}
        with-needs (fn [needs]
                     (assoc-in db [:filters :access-needs] needs))]
    (is (= "Appointments are hidden"
           (cal/access-needs-filter-summary (assoc-in db [:filters :appointments?] false))))
    (is (= "Showing all appointments"
           (cal/access-needs-filter-summary db)))
    (is (= "Showing appointments that need Love"
           (cal/access-needs-filter-summary (with-needs #{1}))))
    (is (= "Showing appointments that need Food"
           (cal/access-needs-filter-summary (with-needs #{3}))))
    (is (= "Showing appointments that need Love OR Fulfillment"
           (cal/access-needs-filter-summary (with-needs #{1 2}))))
    (is (= "Showing appointments that need Love, Fulfillment, OR Food"
           (cal/access-needs-filter-summary (with-needs #{1 2 3}))))
    (is (= "Showing appointments that need Love, Fulfillment, Food, OR Shelter"
           (cal/access-needs-filter-summary (with-needs #{1 2 3 4}))))))