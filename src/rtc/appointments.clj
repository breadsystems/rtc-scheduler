(ns rtc.appointments
  (:require
    [clojure.string :as string]
    [systems.bread.alpha.core :as bread]
    [systems.bread.alpha.component :refer [defc]]
    [systems.bread.alpha.database :as db]
    [systems.bread.alpha.i18n :as i18n]

    [rtc.admin :as admin]
    [rtc.ui :as ui])
  (:import
    [java.time LocalDateTime ZoneId]
    [java.text SimpleDateFormat]
    [java.util UUID]))

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
                               :thing/keys [created-at
                                            updated-at
                                            uuid]
                               :appt/keys [pronouns
                                           scheduled-for
                                           preferred-comm
                                           text-ok?
                                           available-days
                                           available-times
                                           notes]}]
  (some->
    appt
    (assoc
      :appt/notes (reverse (sort-by :thing/created-at notes))
      :info/name-and-pronouns (str (:appt/name appt)
                                   (when (seq pronouns)
                                     (str " (" pronouns ")")))
      :info/updated-at (when updated-at (.format fmt updated-at))
      :info/updated-days-ago (when updated-at
                               (days-between (Date->LocalDateTime updated-at) now))
      :info/created-at (when created-at (.format fmt created-at))
      :info/created-days-ago (when created-at
                               (days-between (Date->LocalDateTime created-at) now))
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
    (when updated-days-ago
      [:.days-ago "🕗 last updated " (in-days updated-days-ago)])]
   [:h2 (:appt/alias appt) " in " (state->label state)]
   [:.appt-summary
    (when created-days-ago
      [:div
       [:.field-label "First requested"]
       [:.field-value (in-days created-days-ago)]])
    [:div
     [:.field-label "Last note from"]
     [:.field-value last-note-from]]
    ]
   [:.flex
    [:.spacer]
    [:div
     [:a {:href uri} "Details"]]]])

(defc AppointmentsList [{:keys [appointments filters] :as data}]
  {:key :appointments
   :query '[*
            {:appt/access-needs [*]}
            {:appt/notes [* {:note/created-by [:user/name]}]}]}
  (let [{:keys [status state]} filters
        any-filters? (or status state)]
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
               (if (zero? (count appointments))
                 [:div "No appointments found."]
                 [:div "Listing " (count appointments) " appointments"])
               (map AppointmentCard appointments)]]))))

(defmethod bread/expand ::annotate
  [{:keys [now] :as expansion} data]
  (let [appts (map first (get data (:expansion/key expansion)))]
    (map (partial annotate {:now now}) appts)))

(defmethod bread/dispatch ::show-all
  [{:keys [params] ::bread/keys [dispatcher] :as req}]
  (let [filters (admin/coerce-filter-params params filter-coercions)
        query {:find [(list 'pull '?e (:dispatcher/pull dispatcher))]
               :where (filter seq [['?e :appt/name] ;; TODO post/type
                                   (when (:status filters)
                                     ['?e :post/status (:status filters)])
                                   (when (:state filters)
                                     ['?e :appt/state (:state filters)])])}]
    {:expansions
     [{:expansion/key :filters
       :expansion/name ::bread/value
       :expansion/value filters
       :expansion/description "The currently applied search filters"}
      {:expansion/key :appointments
       :expansion/name ::db/query
       :expansion/description "Query appointments."
       :expansion/db (db/database req)
       :expansion/args [query]}
      {:expansion/key :appointments
       :expansion/name ::annotate
       :now (LocalDateTime/now)
       :expansion/description "Annotate appointment with view-layer data"}]}))

(defn AccessNeed [{:as need :need/keys [type met?]}]
  [:.access-need
   (if met? "✓" "✗")
   " " (need-type->label type)])

(defn AppointmentNote [{:as note
                        :thing/keys [created-at]
                        :note/keys [created-by
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
      (when created-at
        [:div
         [:.field-label "First requested"]
         [:.field-value created-at]])
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
  {:query '[*
            {:appt/access-needs [*]}
            {:appt/notes [:thing/created-at
                          :note/content
                          {:note/created-by [:user/name]}]}
            {:thing/fields [*]}]
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

;; TODO upstream to bread.thing
(defn by-uuid-expansion
  ([req]
   (query-by-uuid req {:params-key :thing/uuid}))
  ([{:as req ::bread/keys [dispatcher]}
    {k :params-key :or {k :thing/uuid}}]
   (let [uuid (UUID/fromString (get (:route/params dispatcher) k))
         query {:find [(list 'pull '?e (:dispatcher/pull dispatcher)) '.]
                :in '[$ ?uuid]
                :where '[[?e :thing/uuid ?uuid]]}
         expansion {:expansion/key (:dispatcher/key dispatcher)
                    :expansion/name ::db/query
                    :expansion/db (db/database req)
                    :expansion/args [query uuid]
                    ;; TODO observe this flag from i18n
                    :expansion/i18n? (:dispatcher/i18n? dispatcher)}]
     {:expansions (bread/hook req ::i18n/expansions expansion)})))

;; TODO ::thing/by-uuid
;; TODO ::<...> expansion convention?
(defmethod bread/dispatch ::by-uuid [req]
  (by-uuid-expansion req))
