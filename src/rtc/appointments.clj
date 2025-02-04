(ns rtc.appointments
  (:require
    [clojure.string :as string]
    [systems.bread.alpha.core :as bread]
    [systems.bread.alpha.component :refer [defc]]

    [rtc.admin :as admin]
    [rtc.ui :as ui])
  (:import
    [java.time LocalDateTime ZoneId]
    [java.text SimpleDateFormat]))

(defn days-between [a b]
  (.between (java.time.temporal.ChronoUnit/DAYS) a b))

(defn Date->LocalDateTime [dt]
  (LocalDateTime/ofInstant (.toInstant dt) (ZoneId/systemDefault)))

(comment
  (.between (java.time.temporal.ChronoUnit/DAYS)
            (LocalDateTime/now)
            (LocalDateTime/ofInstant (.toInstant #inst "2024-11-01T00:00:00")
                                     (ZoneId/systemDefault)))
  (days-between (LocalDateTime/now)
                (Date->LocalDateTime #inst "2024-11-01T00:00:00")))

(def filter-coercions
  {:status #(when (seq %) (keyword %))
   :state #(when (seq %) (keyword %))})

(def $archived
  [])

(def $appointments
  [{:appt/uuid 123
    :appt/created-at #inst "2024-11-12T09:06:00-07:00"
    :appt/updated-at #inst "2024-11-13T03:46:00-07:00"
    :appt/name "Angela Davis"
    :appt/alias "A."
    :appt/pronouns "she/her"
    :appt/email "aydavis@ucsc.edu"
    :appt/phone "+19165551234"
    :appt/state :CA
    :appt/status :needs-attention
    :appt/text-ok? true
    :appt/preferred-comm :text
    :appt/available-days #{:mon :tues :weds :thurs :fri :sat :sun}
    :appt/available-times #{"Mornings" "Afternoons" "Evenings"}
    :appt/provider {:provider/name "Ruha Benjamin"
                    :provider/title "MD, PDO"
                    :provider/specialty "MD, PDO"
                    :provider/pronouns "she/her"
                    :provider/email "ruha@princeton.edu"
                    :provider/phone "+12535559876"
                    :provider/licensure #{{:license/state "MD"}}}
    :appt/reason "HRT"
    :appt/access-needs [{:need/type :need.type/captioning
                         :need/met? false}]
    :appt/notes []
    :appt/misc "Lorem ipsum dolor sit amet"}
   {:appt/uuid 234
    :appt/created-at #inst "2024-11-12T09:06:00-07:00"
    :appt/updated-at #inst "2024-11-13T03:46:00-07:00"
    :appt/name "Bobby Seale"
    :appt/alias "B."
    :appt/pronouns "he/him"
    :appt/email "bobby@panthers.social"
    :appt/phone "+17245556859"
    :appt/state :WA
    :appt/status :waiting
    :appt/text-ok? true
    :appt/preferred-comm :text
    :appt/available-days #{:mon :weds :fri}
    :appt/available-times #{"Afternoons"}
    :appt/provider {:provider/name "Ruha Benjamin"
                    :provider/title "MD, PDO"
                    :provider/specialty "MD, PDO"
                    :provider/pronouns "she/her"
                    :provider/email "ruha@princeton.edu"
                    :provider/phone "+12535559876"
                    :provider/licensure #{{:license/state "MD"}}}
    :appt/reason "ADHD prescription"
    :appt/access-needs [{:need/type :need.type/captioning
                         :need/met? true}]
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"
                  :note/content "Need to schedule live captioner"}]}
   {:appt/uuid 345
    :appt/created-at #inst "2024-11-10T08:02:00-07:00"
    :appt/updated-at #inst "2024-11-11T05:42:00-07:00"
    :appt/name "Cornel"
    :appt/alias "C."
    :appt/pronouns "he/him"
    :appt/email "c@example.com"
    :appt/phone "+17245556859"
    :appt/state :WA
    :appt/status :waiting
    :appt/text-ok? true
    :appt/preferred-comm :text
    :appt/available-days #{}
    :appt/available-times #{"Evenings"}
    :appt/reason "HRT"
    :appt/access-needs []
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"
                  :note/content "Need to schedule live captioner"}]}
   {:appt/uuid 456
    :appt/scheduled-for #inst "2024-11-19T17:00:00-07:00"
    :appt/scheduled-at #inst "2024-11-13T03:46:00-07:00"
    :appt/created-at #inst "2024-11-12T09:06:00-07:00"
    :appt/updated-at #inst "2024-11-13T03:46:00-07:00"
    :appt/name "Someone"
    :appt/alias "G."
    :appt/pronouns "they/them"
    :appt/email "someone@example.com"
    :appt/phone "+17145555896"
    :appt/state :WA
    :appt/status :scheduled
    :appt/text-ok? true
    :appt/preferred-comm :text
    :appt/provider {:provider/name "Ruha Benjamin"
                    :provider/title "MD, PDO"
                    :provider/specialty "MD, PDO"
                    :provider/pronouns "she/her"
                    :provider/email "ruha@princeton.edu"
                    :provider/phone "+12535559876"
                    :provider/licensure #{{:license/state "MD"}}}
    :appt/reason "ADHD prescription"
    :appt/access-needs [{:need/type :need.type/captioning
                         :need/met? true}]
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-11T12:17:00-05:00"
                  :note/content "Doing stuff..."}
                 {:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"
                  :note/content "All set!"}]}
   {:appt/uuid 567
    :appt/scheduled-for #inst "2024-11-15T17:00:00-07:00"
    :appt/scheduled-at #inst "2024-11-13T03:46:00-07:00"
    :appt/created-at #inst "2024-11-12T09:06:00-07:00"
    :appt/updated-at #inst "2024-11-13T03:46:00-07:00"
    :appt/name "Bobby Seale"
    :appt/alias "D."
    :appt/pronouns "he/him"
    :appt/email "bobby@panthers.social"
    :appt/phone "+17245556859"
    :appt/state :WA
    :appt/status :archived
    :appt/text-ok? true
    :appt/preferred-comm :text
    :appt/provider {:provider/name "Ruha Benjamin"
                    :provider/title "MD, PDO"
                    :provider/specialty "MD, PDO"
                    :provider/pronouns "she/her"
                    :provider/email "ruha@princeton.edu"
                    :provider/phone "+12535559876"
                    :provider/licensure #{{:license/state "MD"}}}
    :appt/reason "HRT"
    :appt/access-needs [{:need/type :need.type/captioning
                         :need/met? true}]
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"
                  :note/content "Need to schedule live captioner"}]}
   {:appt/uuid 678
    :appt/created-at #inst "2024-11-10T08:02:00-07:00"
    :appt/updated-at #inst "2024-11-11T05:42:00-07:00"
    :appt/alias "E."
    :appt/pronouns "they/them"
    :appt/email "c@example.com"
    :appt/phone "+17245556859"
    :appt/state :WA
    :appt/status :archived
    :appt/text-ok? true
    :appt/preferred-comm :text
    :appt/reason "HRT"
    :appt/access-needs []
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"
                  :note/content "Need to schedule live captioner"}]}])

(defn get-appointments [db {:keys [status state]}]
  (->> db
       (filter (fn [appt]
                 (and (or (and (nil? status)
                               (not= :archived (:appt/status appt)))
                          (= status (:appt/status appt)))
                      (or (nil? state) (= state (:appt/state appt))))))
       (sort-by :appt/created-at)))

(defn uuid->appointment [db uuid]
  (->> db
       (reduce (fn [_ appt]
                 (when (= (str uuid) (str (:appt/uuid appt)))
                   (reduced appt)))
               nil)))

;; TODO ^^^ REFACTOR WITH A REAL DATABASE

(def $appt-statuses [:needs-attention :waiting :scheduled :archived])

(def $days [:mon :tues :weds :thurs :fri :sat :sun])

(def status->label
  {:needs-attention "Needs attention"
   :waiting "Waiting"
   :scheduled "Scheduled"
   :archived "Archived"})

(def state->label
  {:CA "California"
   :WA "Washington"})

(def need-type->label
  {:need.type/captioning "Captioning"})

(def day->label
  {:mon "Monday"
   :tues "Tuesday"
   :weds "Wednesday"
   :thurs "Thursday"
   :fri "Friday"
   :sat "Saturday"
   :sun "Sunday"})

(defn days->labels [days]
  (->> $days (filter (set days)) (map day->label)))

(defn summarize-days [days]
  (cond
    (empty? days) "No days specified"
    (= 7 (count days)) "Any day"
    (= #{:mon :tues :weds :thurs :fri} (set days)) "Weekdays"
    (= #{:sat :sun} (set days)) "Weekends"
    :else (string/join ", " (days->labels days))))

(comment
  (summarize-days [:tues :mon])
  (summarize-days nil)
  (summarize-days [])
  (summarize-days [:sat :sun])
  (summarize-days [:mon :tues :weds :thurs :fri])
  (summarize-days [:mon :tues :weds :thurs :fri :sat :sun]))

(defn in-days [n]
  (cond
    (> n 1) (str n " days ago")
    (= n 1) "Yesterday"
    (= n 0) "Today"
    (= n -1) "Tomorrow"
    (< n 0) (str (abs n) " days from now")))

(defn access-needs-met? [{:appt/keys [access-needs]}]
  (reduce #(if (:need/met? %2) %1 (reduced false)) true access-needs))

(def fmt (SimpleDateFormat. "EEE, LLL d 'at' h:mm a"))
(def note-fmt (SimpleDateFormat. "LLL d 'at' h:mm a"))

(defn annotate [{:keys [now]} {:as appt
                               :appt/keys [pronouns
                                           updated-at
                                           created-at
                                           scheduled-for
                                           preferred-comm
                                           text-ok?
                                           available-days
                                           available-times
                                           notes
                                           uuid]}]
  (some->
    appt
    (assoc
      :appt/notes (reverse (sort-by :note/created-at notes))
      :info/name-and-pronouns (str (:appt/name appt)
                                   (when (seq pronouns)
                                     (str " (" pronouns ")")))
      :info/updated-at (.format fmt updated-at)
      :info/updated-days-ago (days-between (Date->LocalDateTime updated-at) now)
      :info/created-at (.format fmt created-at)
      :info/created-days-ago (days-between (Date->LocalDateTime created-at) now)
      :info/scheduled-for (when scheduled-for
                            (.format fmt scheduled-for))
      :info/scheduled-for-days (when scheduled-for
                                 (days-between (Date->LocalDateTime scheduled-for) now))
      :info/note-count (case (count notes)
                         0 "No notes"
                         1 "1 note"
                         (str (count notes) " notes"))
      :info/last-note-from (if (seq notes)
                             (get-in (last notes) [:note/created-by :user/name])
                             "No notes")
      :info/uri (str "/admin/appointments/" uuid)
      :info/preferred-comm (when preferred-comm
                             (string/capitalize (name preferred-comm)))
      :info/text-ok? (string/capitalize (ui/yes-or-no text-ok?))
      :info/available-days (summarize-days available-days)
      :info/available-times (if available-times
                              (string/join ", " available-times)
                              "No times specified")
      :info/all-access-needs-met? (access-needs-met? appt)
      :info/access-needs-summary (if (access-needs-met? appt)
                                   "Access needs met"
                                   "Unmet access needs")
      :info/access-needs-icon (if (access-needs-met? appt) "✓" "♿"))))

(defn AppointmentCard [{:as appt
                        :appt/keys [status state email phone]
                        :info/keys [alias
                                    all-access-needs-met?
                                    access-needs-icon
                                    access-needs-summary
                                    updated-days-ago
                                    created-days-ago
                                    scheduled-for-days
                                    last-note-from
                                    uri]
                        :or {status ""}}]
  [:article.card.appointment {:data-status (name status)}
   [:.status-line.flex
    [:.appt-status (status->label status)]
    (when scheduled-for-days
      [:.appt-scheduled-for "📅 " (in-days scheduled-for-days)])
    [:.appt-access-needs {:class (when-not all-access-needs-met? :unmet)}
     access-needs-icon " " access-needs-summary]
    [:.spacer]
    [:.days-ago "🕗 last updated " (in-days updated-days-ago)]]
   [:h2 (:appt/alias appt) " in " (state->label state)]
   [:.appt-summary
    [:div
     [:.field-label "First requested"]
     [:.field-value (in-days created-days-ago)]]
    [:div
     [:.field-label "Last note from"]
     [:.field-value last-note-from]]
    ]
   [:.flex
    [:.spacer]
    [:div
     [:a {:href uri} "Details"]]]])

(defc AppointmentsList [{:keys [appointments filters now system] :as data}]
  {:key :appointments
   :query [:appt/*]}
  (let [{:keys [status state]} filters
        any-filters? (or status state)
        ;; TODO annotate in an expansion
        appts (map (partial annotate {:now now}) appointments)]
    (admin/AdminPage
      (assoc data
             :title "Appointments"
             :content
             [:<>
              [:aside
               [:h1 "Appointments"]
               [:form.filter-form {:method :get
                                   :action "/admin/appointments"}
                [:fieldset
                 [:legend "Filter by..."]
                 [:label {:for "appt-status"} "Status"]
                 [:select#appt-status {:name :status}
                  [:option {:value "" :label "All outstanding"}]
                  (map (partial ui/Option status->label status) $appt-statuses)]
                 [:label {:for "appt-state"} "State"]
                 [:select#appt-state {:name :state}
                  [:option {:value "" :label "Any state"}]
                  (map (partial ui/Option state->label state) [:CA
                                                               :WA])]
                 [:div
                  [:button {:type :submit}
                   "Filter appointments"]]]]]
              [:main
               (when any-filters?
                 [:.applied-filters
                  (when status
                    [:a {:href (ui/filters->query-string (dissoc filters :status))}
                     (status->label status)])
                  (when state
                    [:a {:href (ui/filters->query-string (dissoc filters :state))}
                     (state->label state)])
                  [:span
                   [:a {:href "/admin/appointments"} "Clear filters"]]])
               (if (zero? (count appts))
                 [:div "No appointments found."]
                 [:div "Listing " (count appts) " appointments"])
               (map AppointmentCard appts)]]))))

(defmethod bread/dispatch ::show-all
  [{:keys [params]}]
  {:expansions
   [{:expansion/key :filters
     :expansion/name ::bread/value
     :expansion/value (admin/coerce-filter-params params filter-coercions)
     :expansion/description "The currently applied search filters"}
    {;; TODO query the db
     :expansion/key :appointments
     :expansion/name ::bread/value
     :expansion/value $appointments
     :expansion/description "All appointments"}]})

(defn AccessNeed [{:as need :need/keys [type met?]}]
  [:.access-need
   (if met? "✓" "✗")
   " " (need-type->label type)])

(defn AppointmentNote [{:as note
                        :note/keys [created-at
                                    created-by
                                    content]}]
  [:.note
   [:.byline.flex
    [:div (:user/name created-by)]
    [:.instruct (.format note-fmt created-at)]]
   [:.content content]])

(defn AppointmentDetails [{:as appt
                           :appt/keys [status
                                       state
                                       notes
                                       access-needs
                                       phone
                                       email
                                       reason
                                       misc]
                           :info/keys [name-and-pronouns
                                       created-at
                                       updated-at
                                       scheduled-for
                                       preferred-comm
                                       text-ok?
                                       available-days
                                       available-times
                                       access-needs-summary
                                       note-count]}]
  (let [available-statuses (filter #(not= status %) $appt-statuses)]
    [:article.appointment.flex.col
     [:header.flex
      [:div
       [:h1 name-and-pronouns]]
      [:.spacer]
      [:.status {:data-status status} (status->label status)]]
     [:.flex
      [:div
       [:.field-label "First requested"]
       [:.field-value created-at]]
      [:.spacer]
      (if scheduled-for
        [:div
         [:.field-label "Scheduled for"]
         [:.field-value scheduled-for]]
        [:div
         [:.instruct "Not scheduled"]])]
     [:section
      [:div [:h2 "Contact"]]
      [:.appt-summary
       [:div
        [:.field-label "State"]
        [:.field-value (state->label state)]]
       [:div
        [:.field-label "Email"]
        [:.field-value email]]
       [:div
        [:.field-label "Phone"]
        [:.field-value phone]]
       [:div
        [:.field-label "Preferred Comm."]
        [:.field-value preferred-comm]]
       [:div
        [:.field-label "Text OK?"]
        [:.field-value text-ok?]]]
      [:div [:h3 "Availability"]]
      [:.appt-availability
       [:div
        [:.field-label "Days"]
        [:.field-value available-days]]
       [:div
        [:.field-label "Times"]
        [:.field-value available-times]]]]
     [:section.medical-needs
      [:header
       [:h2 "Medical needs"]]
      [:div
       [:.field-label "Reason for contacting RTC"]
       [:.field-value reason]]]
     [:section.access-needs
      [:header
       [:h2 "Access needs"]
       [:div access-needs-summary]]
      (when (seq access-needs)
        (map AccessNeed access-needs))]
     [:section.access-needs
      [:header
       [:h2 "Miscellaneous"]]
      [:div
       [:.field-label "Anything we forgot to ask?"]
       (if misc
         [:.field-value misc]
         [:.field-value.instruct "Nothing specified"])]]]))

(defn AppointmentActions [{:appt/keys [notes status]
                           :info/keys [note-count updated-at]}]
  (let [available-statuses (filter #(not= status %) $appt-statuses)]
    [:<>
     [:section.actions
      [:.flex
       [:div [:h2 "Actions"]]
       [:.spacer]
       [:div
        [:.field-label "Last updated"]
        [:.field-value updated-at]]]
      [:form.flex.col
       [:.flex
        [:input {:name :scheduled-for
                 :type :datetime-local}]
        [:button {:type :submit}
         "Schedule"]]]
      [:form.flex.col
       [:.flex
        [:select {:name :status}
         (map (partial ui/Option status->label status) available-statuses)]
        [:button {:type :submit}
         "Update status"]]]]
     [:section.notes
      [:h2 "Notes"]
      [:header.flex.row
       [:h3 note-count]]
      (when (seq notes)
        [:.notes-container
         (map AppointmentNote notes)])
      [:form.action-form {:method :post
                          :name :add-note
                          :data-action {:hello true}}
       [:h3 "Add a note"]
       [:textarea {:name :note-content
                   :rows 5}]
       [:div
        [:button {:type :submit}
         "Add"]]]]]))

(defc AppointmentPage [{:keys [appt now] :as data}]
  {:query []
   :key :appt}
  (let [appt (annotate {:now now} appt)] ;; TODO expansion
    (admin/AdminPage
      (assoc data
             :title "Appointment"
             :container-class :appt-details
             :content
             (if appt
               [:<>
                [:main (AppointmentDetails appt)]
                [:aside (AppointmentActions appt)]]
               [:main
                [:h2 "Appointment not found."]
                [:div [:a {:href "/admin/appointments"} "All appointments"]]])
             :footer
             [:details
              [:summary "Debug info"]
              [:pre (with-out-str (clojure.pprint/pprint appt))]]))))

(defmethod bread/dispatch ::show [{{params :route/params} ::bread/dispatcher}]
  {:expansions
   [{:expansion/key :appt
     :expansion/name ::bread/value
     :expansion/value (uuid->appointment $appointments (:thing/uuid params))}]})
