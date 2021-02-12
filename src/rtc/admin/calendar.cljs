(ns rtc.admin.calendar
  (:require
   [clojure.set :refer [union]]
   [clojure.string :refer [join]]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/interaction" :default interactionPlugin]
   ["@fullcalendar/list" :default listPlugin]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["moment" :as moment]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [rtc.style.colors :as colors]
   [rtc.rest.core :as rest]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;         Core Logic        ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Core logic for availabilities, appointments, and calendar events
;;

(defn appointment? [e]
  (or (= :appointment (:event/type e))
      (= "appointment" (.. e -extendedProps -type))))

(defn availability? [e]
  (or (= :availability (:event/type e))
      (= "availability" (.. e -extendedProps -type))))

(defn overlaps-any? [{:keys [start end]} existing]
  (let [avail-start (inst-ms start)
        avail-end (inst-ms end)
        overlaps? (fn [{:keys [start end]}]
                    (let [start (inst-ms start)
                          end (inst-ms end)]
                      (or
                       (< avail-start start avail-end)
                       (< avail-start end avail-end)
                       (< start avail-start end))))]
    (not (empty? (filter overlaps? existing)))))

(defn filter-by-id [avails id]
  (filter #(= id (:user/id %)) avails))

(defn can-overlap? [a b]
  (let [provider-id #(.. ^js % -_def -extendedProps -provider_id)]
    (or (not (availability? a))
        (not (availability? b))
        (not= (provider-id a) (provider-id b)))))

(defn update-availability [db avail]
  (update-in db [:availabilities (:id avail)] merge avail))

(defn delete-availability [db id]
  (update db :availabilities dissoc (js/parseInt id)))

(defn users-by-id [{:keys [users colors]}]
  (into {} (map (fn [[id user] color]
                  [id (assoc user :color color)])
                users
                (take (count users) (cycle colors)))))

(defn providers [db]
  ;; TODO filter by role?
  (vals (users-by-id db)))

(defn current-user [{:keys [users user-id]}]
  (get users user-id))

(defn focused-appointment [{:keys [appointments focused-appointment]}]
  (some->> focused-appointment
           (get appointments)))

(defn current-access-needs [db]
  (when-let [{:keys [access-needs]} (focused-appointment db)]
    (vals access-needs)))

(defn user->name [{:keys [first_name last_name]}]
  (str first_name " " last_name))

(defn- by-provider [events providers]
  (filter #(contains? providers (:user/id %)) events))

(defn- access-needs [db]
  (vals (:needs db)))

(defn- oxford-comma [phrases]
  (if (> (count phrases) 2)
    (join ", OR " [(join ", " (butlast phrases)) (last phrases)])
    (join " OR " phrases)))

(defn access-needs-filter-summary [{:keys [filters needs]}]
  (let [hiding-appointments? (not (:appointments? filters))
        filtered (:access-needs filters)]
    (cond
      hiding-appointments?
      "Appointments are hidden"

      (seq filtered)
      (let [labels (map :name (filter #(contains? filtered (:id %)) (vals needs)))
            oxford-comma (oxford-comma labels)]
        (str "Showing appointments that need " oxford-comma))

      :else
      "Showing all appointments")))

(defn- fulfilled? [{:keys [access-needs]} need-id]
  (boolean (get-in access-needs [need-id :need/fulfilled?])))

(defn- needs? [appt need]
  (and (contains? (set (map :need/id (vals (:access-needs appt)))) need)
       (not (fulfilled? appt need))))

(defn- needs-any? [appt needs]
  (if (seq needs)
    (let [predicates (map #(fn [appt] (needs? appt %)) needs)]
      (boolean ((apply some-fn predicates) appt)))
    false))

(defn- any-unfulfilled? [{:keys [access-needs]}]
  (boolean (seq (filter (complement :need/fulfilled?) (vals access-needs)))))

(comment
  (def needs-both
    {:access-needs {:interpretation
                    #:need{:id :interpretation :fulfilled? false}
                    :other
                    #:need{:id :other :fulfilled? false}}})
  (def needs-other
    {:access-needs {:interpretation
                    #:need{:id :interpretation :fulfilled? true}
                    :other
                    #:need{:id :other :fulfilled? false}}})
  (def needs-interpreter
    {:access-needs {:interpretation
                    #:need{:id :interpretation :fulfilled? false}
                    :other
                    #:need{:id :other :fulfilled? true}}})
  (def needs-neither
    {:access-needs {:interpretation
                    #:need{:id :interpretation :fulfilled? true}
                    :other
                    #:need{:id :other :fulfilled? true}}})
  (def no-access-needs {:access-needs {}})

  (fulfilled? needs-both :interpretation)
  ;; => false
  (fulfilled? needs-other :interpretation)
  ;; => true
  (fulfilled? needs-other :other)
  ;; => false
  (fulfilled? needs-neither :interpretation)
  ;; => true
  (any-unfulfilled? needs-both)
  ;; => true
  (any-unfulfilled? needs-interpreter)
  ;; => true
  (any-unfulfilled? needs-other)
  ;; => true
  (any-unfulfilled? needs-neither)
  ;; => false
  (any-unfulfilled? no-access-needs)
  ;; => false

  (needs? needs-both :interpretation)
  ;; => true
  (needs? needs-interpreter :interpretation)
  ;; => true
  (needs? needs-other :interpretation)
  ;; => false
  (needs-any? needs-both [:other])
  ;; => true
  (needs-any? needs-both [:interpretation])
  ;; => true
  (needs-any? needs-both [:other :interpretation])
  ;; => true
  (needs-any? needs-neither [:other :interpretation])
  ;; => false
  (needs-any? no-access-needs [:other :interpretation])
  ;; => false

  ;;
  )



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;      Event Visibility     ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Calendar events (appointments, availabilities) logic i.e. whether
;; they are visible in the calendar
;;

(defn- by-access-need [appts needs]
  (if (seq needs)
    (filter #(needs-any? % (set needs)) appts)
    appts))

(defn- full-name [person]
  (or (:name person)
      (join " " (filter some? ((juxt :first_name :last_name) person)))))

(defn- editable-for? [avail user-id]
  (= user-id (:user/id avail)))

(defn- appointment->border-color [appt]
  (if (any-unfulfilled? appt)
    colors/appointment-unfulfilled-border
    colors/appointment-fulfilled-border))

(defn- appointment->bg-color [appt]
  (if (any-unfulfilled? appt)
    colors/appointment-unfulfilled-bg
    colors/appointment-fulfilled-bg))

(defmulti ->fc-event :event/type)

(defmethod ->fc-event :default [e] e)

(defmethod ->fc-event :availability [{:keys [event/provider] :as event}]
  (assoc event
         :title (full-name provider)
         :provider_id (:id provider)
         :backgroundColor (:color provider)
         :borderColor (:color provider)
         :classNames ["rtc-availability" (when (:editable event) "rtc-draggable")]))

(defmethod ->fc-event :appointment [{:keys [event/provider] :as appt}]
  (assoc appt
         :title (full-name appt)
         :provider_id (:id provider)
         :editable false
         :borderColor (appointment->border-color appt)
         :backgroundColor (appointment->bg-color appt)
         :classNames ["rtc-appointment"]))

(defn visible-events [{:keys [availabilities appointments filters needs user-id] :as db}]
  (let [{:keys [availabilities? appointments? providers access-needs]} filters
        ;; Only type and provider filters apply to Availabilities.
        visible-avails (-> (when availabilities?
                             (map #(assoc %
                                          :event/type :availability
                                          :deletable (editable-for? % user-id)
                                          :editable (editable-for? % user-id))
                                  (vals availabilities)))
                           (by-provider providers))
        ;; All filters apply to Appointments.
        visible-appts (-> (when appointments?
                            (map #(assoc % :event/type :appointment)
                                 (vals appointments)))
                          (by-provider providers)
                          ;; Filter by any unfulfilled access needs.
                          (by-access-need access-needs))
        ;; Combine all events and enrich them with de-normalized display data
        enriched (map (fn [event]
                        (assoc event
                               :event/provider (get (users-by-id db) (:user/id event))
                               :event/needs (map #(get needs (:need/id %)) (:access-needs event))))
                      (concat visible-avails visible-appts))]
    (map ->fc-event enriched)))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;      Subscriptions        ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub ::focused-appointment focused-appointment)
(rf/reg-sub ::current-access-needs current-access-needs)
(rf/reg-sub ::note :note)
(rf/reg-sub ::events visible-events)
(rf/reg-sub ::filters :filters)
(rf/reg-sub ::providers providers)
(rf/reg-sub ::user (fn [{:keys [users]} [_ id]]
                     (get users id)))
(rf/reg-sub ::access-needs access-needs)
(rf/reg-sub ::access-need (fn [{:keys [needs]} [_ id]]
                            (get needs id)))
(rf/reg-sub ::access-needs-filter-summary access-needs-filter-summary)
(rf/reg-sub ::can-view-medical-needs? (fn [db]
                                        (:is_provider (current-user db))))

;; Sometimes it's nice to just get the whole db.
;; For debugging only; not used in production code.
(rf/reg-sub ::db identity)

(comment
  (get-in @(rf/subscribe [::db]) [:appointments 1])

  @(rf/subscribe [::can-view-medical-needs?])
  @(rf/subscribe [::focused-appointment])
  @(rf/subscribe [::note])
  @(rf/subscribe [::events])
  @(rf/subscribe [::filters])
  @(rf/subscribe [::providers])
  @(rf/subscribe [::access-needs-filter-summary]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;      Event Handlers       ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti update-filter* (fn [k & _] k))

(defmethod update-filter* :appointments? [_ current _]
  (not current))

(defmethod update-filter* :availabilities? [_ current _]
  (not current))

(defmethod update-filter* :providers [_ providers id]
  (if (contains? providers id)
    (disj providers id)
    (conj providers id)))

(defmethod update-filter* :access-needs [_ needs id]
  (if (contains? needs id)
    (disj needs id)
    (conj needs id)))

(defn update-filter [db [_ k v]]
  (update-in db [:filters k] #(update-filter* k % v)))

(defn clear-filter [db [_ k]]
  (assoc-in db [:filters k] #{}))

(rf/reg-event-fx
 ::create-availability
 (fn [{:keys [db]} [_ avail]]
   (let [{:keys [user-id csrf-token]} db
         avail (merge {:user/id user-id} avail)]
     {::create-availability! {:availability avail
                              :csrf-token csrf-token}})))

(rf/reg-event-fx
 ::update-availability
 (fn [{:keys [db]} [_ avail]]
   (let [{:keys [csrf-token]} db
         uid (get-in db [:availabilities (js/parseInt (:id avail)) :user/id])]
     {::update-availability! {:availability (assoc avail :user/id uid)
                              :csrf-token csrf-token}})))

(rf/reg-event-fx
 ::delete-availability
 (fn [{:keys [db]} [_ id]]
   (let [{:keys [csrf-token]} db
         avail (get-in db [:availabilities (js/parseInt id)])]
     {::delete-availability! {:availability avail
                              :csrf-token csrf-token}})))

;; TODO use interceptors to inject CSRF token
(rf/reg-fx
 ::create-availability!
 (fn [{:keys [availability csrf-token]}]
   (rest/post! "/api/v1/admin/availability"
               {:transit-params availability
                :headers {"x-csrf-token" csrf-token}}
               ::availability-created)))

(rf/reg-fx
 ::update-availability!
 (fn [{:keys [availability csrf-token]}]
   (rest/patch! "/api/v1/admin/availability"
                {:transit-params availability
                 :headers {"x-csrf-token" csrf-token}}
                ::availability-updated)))

(rf/reg-fx
 ::delete-availability!
 (fn [{:keys [availability csrf-token]}]
   (rest/delete! "/api/v1/admin/availability"
                 {:transit-params availability
                  :headers {"x-csrf-token" csrf-token}}
                 ::availability-deleted)))

(rf/reg-event-db ::update-filter (fn [db dispatch]
                                   (update-filter db dispatch)))

(rf/reg-event-fx ::clear-filter (fn [db filter-key]
                                  (clear-filter db filter-key)))

;; Dispatched once the schedule data is ready
(rf/reg-event-fx
 :calendar/load
 (fn [{:keys [db]} [_ {:keys [data errors]}]]
   (if (seq errors)
     ;; TODO some kind of real error handling
     (do (prn errors) {:db db})
     {:db (-> db
              (assoc
               :user-id        (:user-id data)
               :users          (:users data)
               :my-invitations (:invitations data)
               :availabilities (:availabilities data)
               :appointments   (:appointments data))
              (assoc-in
               [:filters :providers]
               (union (set (map :user/id (vals (:availabilies data))))
                      (set (map :user/id (vals (:appointments data)))))))})))

(rf/reg-event-fx
 ::availability-created
 (fn [{:keys [db]} [_ {:keys [data]}]]
   (let [avail (:availability data)]
     {:db (-> db
              (assoc-in [:availabilities (:id avail)] avail)
              ;; Make the current user visible in the filters
              ;; if they weren't previously.
              (update-in [:filters :providers] conj (:user/id avail)))})))

(rf/reg-event-db
 ::availability-updated
 (fn [db [_ {:keys [data]}]]
   (let [avail (:availability data)]
     (update-availability db avail))))

(rf/reg-event-db
 ::availability-deleted
 (fn [db [_ {:keys [data]}]]
   (let [avail (:availability data)]
     (delete-availability db (:id avail)))))

(rf/reg-event-db
 ::need-fulfilled
 (fn [db [_ {{appt-id :appointment/id
              need-id :need/id
              fulfilled? :need/fulfilled?}
             :data}]]
   (let [coord [:appointments appt-id :access-needs need-id :need/fulfilled?]]
     (assoc-in db coord fulfilled?))))


;; Appointment details

(rf/reg-event-db ::update-note (fn [db [_ note]]
                                 (assoc db :note note)))

(rf/reg-event-fx ::create-note (fn [{{:keys [csrf-token user-id focused-appointment note]} :db}]
                                 (let [data {:note {:note note
                                                    :user/id user-id
                                                    :appointment/id focused-appointment}
                                             :csrf-token csrf-token}]
                                   {::create-note! data})))

(rf/reg-event-fx ::fulfill (fn [{{:keys [csrf-token]} :db} [_ appt-need]]
                             (let [data {:appt-need appt-need
                                         :csrf-token csrf-token}]
                               {::fulfill! data})))

(rf/reg-event-fx ::focus-appointment (fn [{:keys [db]} [_ id]]
                                       {:db (assoc db :focused-appointment id)
                                        ::get-appointment-details id}))

(rf/reg-fx
 ::get-appointment-details
 (fn [id]
   (when id
     (rest/get! "/api/v1/admin/appointment"
                {:query-params {:id id}}
                ::merge-appointment-info))))

(rf/reg-fx
 ::create-note!
 (fn [{:keys [note csrf-token]}]
   (rest/post! "/api/v1/admin/appointment/note"
               {:transit-params note
                :headers {"x-csrf-token" csrf-token}}
               ::note-created)))

(rf/reg-fx
 ::fulfill!
 (fn [{:keys [appt-need csrf-token]}]
   (rest/post! "/api/v1/admin/appointment/need/fulfill"
               {:transit-params appt-need
                :headers {"x-csrf-token" csrf-token}}
               ::need-fulfilled)))

(rf/reg-event-db
 ::merge-appointment-info
 (fn [db [_ {:keys [data]}]]
   (update-in db [:appointments (:id data)] merge data)))

(rf/reg-event-db
 ::note-created
 (fn [db [_ {:keys [data]}]]
   (-> db
       (update-in [:appointments (:appointment/id data) :notes] #(concat [data] %))
       ;; reset the current note
       (assoc :note ""))))

(comment
  @(rf/subscribe [::filters])
  @(rf/subscribe [::providers])
  @(rf/subscribe [::access-needs-filter-summary])
  @(rf/subscribe [::appointments])

  (rf/dispatch [::update-filter :providers 3])
  (rf/dispatch [::update-filter :providers 4]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;         Components        ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- careseeker-name [person]
  (if (> (count (:name person)) 0)
    [:span (:name person)]
    [:i "Anonymous"]))

(defn modal [children]
  [:div.modal-bg
   [:aside.modal
    [:div.modal__contents
     [:<> children]
     [:span.modal__close {:on-click #(rf/dispatch [::focus-appointment nil])} "×"]]]])

(defn filter-controls []
  (let [filters @(rf/subscribe [::filters])
        access-needs @(rf/subscribe [::access-needs])
        summary @(rf/subscribe [::access-needs-filter-summary])
        providers @(rf/subscribe [::providers])
        showing-appointments? (:appointments? filters)
        can-clear-access-needs? (and showing-appointments?
                                     (seq (:access-needs filters)))]
    [:div.filter-controls
     [:div.filter-group
      [:h4 "Filter by provider"]
      (doall (map (fn [{:keys [id] :as provider}]
                    (let [html-id (str "provider-filter-" id)]
                      ^{:key id}
                      [:div.filter-field
                       [:input {:id html-id
                                :type :checkbox
                                :on-change #(rf/dispatch [::update-filter :providers id])
                                :checked (contains? (:providers filters) id)
                                :style {}}]
                       [:label.filter-label.filter-label--provider
                        {:for html-id
                         :style {:border-color (:color provider)}}
                        (full-name provider)]]))
                  providers))]
     [:div.filter-group
      [:h4 "Filter by access need"]
      (doall (map (fn [{:keys [id name]}]
                    (let [html-id (str "access-filter-" id)]
                      ^{:key id}
                      [:div.filter-field
                       [:input {:id html-id
                                :type :checkbox
                                :on-change #(rf/dispatch [::update-filter :access-needs id])
                                :checked (contains? (:access-needs filters) id)
                                :disabled (not showing-appointments?)
                                :style {}}]
                       [:label.filter-label {:for html-id}
                        (str name)]]))
                  access-needs))
      [:p.instruct summary]
      (when can-clear-access-needs?
        [:div
         [:a.text-button {:on-click #(rf/dispatch [::clear-filter :access-needs])}
          "Show all"]])]
     [:div.filter-group
      [:h4 "Filter by type"]
      [:div.filter-field
       [:input {:id "show-availabilities"
                :type :checkbox
                :on-change #(rf/dispatch [::update-filter :availabilities? nil])
                :checked (:availabilities? filters)}]
       [:label.filter-label {:for "show-availabilities"} "Show availabilities"]]
      [:div.filter-field
       [:input {:id "show-appointments"
                :type :checkbox
                :on-change #(rf/dispatch [::update-filter :appointments? nil])
                :checked (:appointments? filters)}]
       [:label.filter-label {:for "show-appointments"} "Show appointments"]]]
     [:fieldset.access-needs-legend
      [:legend "Appointment colors"]
      [:div.access-needs-indicator.--unmet "Unmet access needs"]
      [:div.access-needs-indicator.--met "Access needs met"]]]))

(defmulti access-need (fn [{:keys [need/type]}] type))

(defmethod access-need :default [{:keys [name]}]
  [:span.access-need-label name])

(defmethod access-need :interpreter [{:keys [name interpreter/lang]}]
  [:div.access-need [:b name] ": " lang])

(comment
  @(rf/subscribe [::focused-appointment]))

(defn- appointment-contact
  [{:keys [email phone ok_to_text preferred_communication_method]}]
  [:div.appointment-field-group
   [:h3 "Contact"]
   [:dl
    [:dt "Email"]
    [:dd
     (if (seq email)
       [:a {:href (str "mailto:" email)} email]
       [:span.help "not given"])]
    [:dt "Phone"]
    [:dd
     (if (seq phone)
       [:a {:href (str "tel:" phone)} phone]
       [:span.help "not given"])]
    [:dt "OK to text?"]
    [:dd (if ok_to_text "yes" "no")]
    [:dt "Preferred comm. method"]
    [:dd preferred_communication_method]]])

(defn- appointment-access-needs [{appt-id :id}]
  (let [access-needs @(rf/subscribe [::current-access-needs])]
    [:div.appointment-field-group
     [:h3 "Access Needs"]
     (if (seq (filter (complement :need/fulfilled?) access-needs))
       [:div.access-needs-indicator.--unmet "Unmet access needs"]
       [:div.access-needs-indicator.--met "Access needs met!"])
     (doall
      (map (fn [need]
             (let [{:need/keys [id info fulfilled?] label :name}
                   (merge @(rf/subscribe [::access-need (:need/id need)])
                          need)]
               ^{:key id}
               [:p
                [:input {:id (str "fulfilled-" (name id))
                         :type :checkbox
                         :on-change #(rf/dispatch
                                      [::fulfill
                                       {:need/id id
                                        :appointment/id appt-id
                                        :need/fulfilled? (not fulfilled?)}])
                         :checked fulfilled?}]
                [:label {:for (str "fulfilled-" (name id))}
                 label ": " info]]))
           access-needs))]))

(defn- medical-needs [{:keys [reason]}]
  (when @(rf/subscribe [::can-view-medical-needs?])
    [:div.appointment-field-group
     [:h3 "Medical Needs"]
     [:p reason]]))

(defn- appointment-notes [{:keys [notes]}]
  (let [current-note @(rf/subscribe [::note])
        confirm?! #(js/confirm (str "Confirm leaving this note? \""
                                    current-note "\""))
        can-create-note? (> (count current-note) 5)]
    [:div.appointment-notes
     [:h3 "Notes"]
     [:div.create-note
      [:textarea.create-note__text {:on-change #(rf/dispatch-sync
                                                  [::update-note
                                                   (.. % -target -value)])
                                    :value current-note}]
      [:p
       [:button.secondary {:on-click
                           #(when (confirm?!)
                                  (rf/dispatch [::create-note]))
                           :title (when-not can-create-note? "Note is too short.")
                           :disabled (not can-create-note?)}
        "Create a note"]]
      [:p.help "Notes cannot be deleted."]]
     (doall (map (fn [{:keys [note date_created] :as appt-note}]
                   (let [user @(rf/subscribe [::user (:user/id appt-note)])]
                     ^{:key date_created}
                     [:aside.appt-note
                      [:div.appt-note__time (.format (moment date_created) "M/D H:mm a")]
                      [:div.appt-note__text note]
                      [:div.appt-note__attribution (user->name user)]]))
                 (vec notes)))]))

(defn appointment-details []
  (let [{:keys [pronouns] :as appt} @(rf/subscribe [::focused-appointment])
        provider @(rf/subscribe [::user (:user/id appt)])
        start (moment. (:start appt))]
    (prn (keys appt))
    [:article.appointment
     [:header
      [:h2.appointment-name
       (careseeker-name appt)
       (when (seq pronouns)
         (str " (" pronouns ")"))]
      [:h3
       (.format start "h:mma ddd, MMM D")
       " with " (:first_name provider) " " (:last_name provider)]]
     [:div.appointment-details
      (appointment-contact appt)
      (appointment-access-needs appt)]
     (medical-needs appt)
     (appointment-notes appt)]))

(defn care-schedule []
  (let [appt @(rf/subscribe [::focused-appointment])]
    [:div.schedule-container
     (when appt
       [modal
        [appointment-details]])
     [:div.care-schedule
      [filter-controls]
      [:> FullCalendar
       {:header-toolbar #js {:left "prev,next today"
                             :center "title"
                             :right "timeGridWeek listWeek"}
        :event-did-mount (fn [^js info]
                           (when (.. info -event -_def -extendedProps -deletable)
                             (let [id (.. info -event -id)
                                   elem (.-el info)
                                   delete-btn (js/document.createElement "i")
                                   on-click #(rf/dispatch [::delete-availability id])]
                               (.addEventListener delete-btn "click" on-click)
                               (set! (.-innerText delete-btn) "×")
                               (.add (.-classList delete-btn) "rtc-delete")
                               (.appendChild elem delete-btn))))
        :selectable true
        :select (fn [event]
                  (rf/dispatch [::create-availability {:start (.-start event)
                                                       :end   (.-end event)}]))
        :default-view "timeGridWeek"
        :events @(rf/subscribe [::events])
        :event-click (fn [info]
                      (let [e (.-event info)
                            id (js/parseInt (.-id e))]
                        (when (appointment? e)
                          (rf/dispatch [::focus-appointment id]))))
        :event-change (fn [info]
                        (let [e (.-event info)
                              id (js/parseInt (.-id e))]
                          (rf/dispatch [::update-availability {:id id
                                                               :start (.-start e)
                                                               :end (.-end e)}])))
        :plugins [interactionPlugin listPlugin timeGridPlugin]}]]]))
