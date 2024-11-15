(ns rtc.appointments
  (:require
    [rtc.ui :as ui]))

(def filter-coercions
  {:status #(when (seq %) (keyword %))
   :state #(when (seq %) (keyword %))})

(def $archived
  [])

(def $appointments
  [{:appt/datetime #inst "2024-11-15T17:00:00-07:00"
    :appt/created-at #inst "2024-11-12T09:06:00-07:00"
    :appt/updated-at #inst "2024-11-13T03:46:00-07:00"
    :appt/name "Angela Davis"
    :appt/alias "A."
    :appt/pronouns "she/her"
    :appt/email "aydavis@ucsc.edu"
    :appt/phone "+19165551234"
    :appt/state :CA
    :appt/status :appt.status/needs-attention
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
    :appt/access-needs [{:need/type :need.type/transcription
                         :need/met? false}]
    :appt/notes [{:note/created-by {:user/name "Danielle"
                                    :user/pronouns "they/them"}
                  :note/created-at #inst "2024-11-12T17:23:00-05:00"}]
    }])

(def status->label
  {:needs-attention "Needs attention"
   :waiting "Waiting"
   :confirmed "Confirmed"
   :archived "Archived"})

(def state->label
  {:CA "California"
   :WA "Washington"})

(defn AppointmentCard [appt]
  [:article.appointment
   [:header
    [:div
     [:span
      (status->label (:appt/status appt))]]
    [:h3 (or (:appt/alias appt))]
    [:div
     [:dl {:style {:display :flex}}
      [:dt "State"]
      [:dd (state->label (:appt/state appt))]
      [:dt "Email"]
      [:dd (:appt/email appt)]
      [:dt "Phone"]
      [:dd (:appt/phone appt)]]]]])

(comment

  (keys $req)
  (:filters $req)

  ;;
  )

(defn show-all [{:keys [params filters] :as req}]
  (let [{:keys [status state]} filters
        any-filters? (or status state)]
    (ui/Page
      :title "Appointments"
      :footer
      [:<> [:script {:src "/js/admin/js"}]]
      :content
      [:main
       [:h1 "Appointments"]
       [:form {:method :get
               :action "/admin/appointments"}
        [:div
         [:div "Filter by..."]
         [:label {:for "appt-status"} "Status"]
         [:select#appt-status {:name :status}
          [:option {:value "" :label "Any status"}]
          (map (partial ui/Option status->label status)
               [:needs-attention
                :waiting
                :confirmed
                :archived])]
         [:label {:for "appt-state"} "State"]
         [:select#appt-state {:name :state}
          [:option {:value "" :label "Any state"}]
          (map (partial ui/Option state->label state)
               [:CA
                :WA])]
         [:button {:type :submit}
          "Filter appointments"]]
        [:div
         (when status
           (ui/FilterChip "Status" (status->label status)
                          (ui/filters->query-string (dissoc filters :status))))
         (when state
           (ui/FilterChip "State" (state->label state)
                          (ui/filters->query-string (dissoc filters :state))))
         (when any-filters?
           [:span
            [:a {:href "/admin/appointments"} "Clear filters"]])]
        [:div
         (map AppointmentCard $appointments)]]
       [:pre [:code (pr-str filters)]]])))

(defn show [_]
  (ui/Page
    :title "Appointment"
    :content
    [:main
     "TODO Single Appointment"]))
