(ns rtc.appointments
  (:require
    [rtc.ui :as ui])
  (:import
    [java.time LocalDateTime ZoneId]))

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
    :appt/notes []}
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
   {:appt/uuid 345
    :appt/created-at #inst "2024-11-10T08:02:00-07:00"
    :appt/updated-at #inst "2024-11-11T05:42:00-07:00"
    :appt/alias "C."
    :appt/pronouns "they/them"
    :appt/email "c@example.com"
    :appt/phone "+17245556859"
    :appt/state :WA
    :appt/status :waiting
    :appt/text-ok? true
    :appt/preferred-comm :text
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
    :appt/email "someone@example.vom"
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
    :appt/reason "HRT"
    :appt/access-needs [{:need/type :need.type/captioning
                         :need/met? true}]
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"
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

(def status->label
  {:needs-attention "Needs attention"
   :waiting "Waiting"
   :scheduled "Scheduled"
   :archived "Archived"})

(def state->label
  {:CA "California"
   :WA "Washington"})

(defn in-days [n]
  (cond
    (> n 1) (str n " days ago")
    (= n 1) "Yesterday"
    (= n 0) "Today"
    (= n -1) "Tomorrow"
    (< n 0) (str (abs n) " days from now")))

(defn access-needs-met? [{:appt/keys [access-needs]}]
  (reduce #(if (:need/met? %2) %1 (reduced false)) true access-needs))

(defn annotate [{:keys [now]} {:as appt :appt/keys [updated-at
                                                    created-at
                                                    scheduled-for
                                                    notes]}]
  (assoc appt
         :info/updated-days-ago (days-between (Date->LocalDateTime updated-at) now)
         :info/created-days-ago (days-between (Date->LocalDateTime created-at) now)
         :info/scheduled-for-days (when scheduled-for
                                    (days-between (Date->LocalDateTime scheduled-for) now))
         :info/note-count (case (count notes)
                            0 "No notes"
                            1 "1 note"
                            (str (count notes) " notes"))
         :info/last-note-from (if (seq notes)
                                (get-in (last notes) [:note/created-by :user/name])
                                "No notes")
         ))

(defn AppointmentCard [{:as appt
                        :appt/keys [status state email phone uuid]
                        :info/keys [updated-days-ago
                                    created-days-ago
                                    scheduled-for-days
                                    last-note-from]
                        :or {status ""}}]
  [:article.card.appointment {:data-status (name status)}
   [:.status-line.flex
    [:.appt-status (status->label status)]
    (when scheduled-for-days
      [:.appt-scheduled-for "📅 " (in-days scheduled-for-days)])
    (if (access-needs-met? appt)
      [:.appt-access-needs.met "✓ Access needs met"]
      [:.appt-access-needs.unmet "♿ Unmet access needs"])
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
     [:a {:href (str "/admin/appointments/" uuid)}
      "Details"]]]])

(defn get-appointments [db {:keys [status state]}]
  (->> db
       (filter (fn [appt]
                 (and (or (and (nil? status)
                               (not= :archived (:appt/status appt)))
                          (= status (:appt/status appt)))
                      (or (nil? state) (= state (:appt/state appt))))))
       (sort-by :appt/created-at)))

(defn show-all [{:keys [params filters now] :as req}]
  (let [db $appointments ;; TODO
        {:keys [status state]} filters
        any-filters? (or status state)
        results (get-appointments db filters)
        appts (map (partial annotate {:now now}) results)]
    (ui/Page
      :title "Appointments"
      :footer
      [:<> [:script {:src "/admin/admin.js"}]]
      :content
      [:main
       [:h1 "Appointments"]
       [:form {:method :get
               :action "/admin/appointments"}
        [:fieldset
         [:legend "Filter by..."]
         [:span
          [:label {:for "appt-status"} "Status"]
          [:select#appt-status {:name :status}
           [:option {:value "" :label "Any status"}]
           (map (partial ui/Option status->label status) [:needs-attention
                                                          :waiting
                                                          :scheduled
                                                          :archived])]]
         [:span
          [:label {:for "appt-state"} "State"]
          [:select#appt-state {:name :state}
           [:option {:value "" :label "Any state"}]
           (map (partial ui/Option state->label state) [:CA
                                                        :WA])]]
         [:button {:type :submit}
          "Filter appointments"]]
        [:.applied-filters
         (when status
           [:a {:href (ui/filters->query-string (dissoc filters :status))}
            (status->label status)])
         (when state
           [:a {:href (ui/filters->query-string (dissoc filters :state))}
            (state->label state)])
         (when any-filters?
           [:span
            [:a {:href "/admin/appointments"} "Clear filters"]])]]
       [:.flex.col
        (if (zero? (count appts))
          [:div "No appointments found."]
          [:div "Listing " (count appts) " appointments"])
        (map AppointmentCard appts)]])))

(defn show [_]
  (ui/Page
    :title "Appointment"
    :content
    [:main
     "TODO Single Appointment"]))
