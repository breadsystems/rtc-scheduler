(ns rtc.admin.calendar
  (:require
   [clojure.set :refer [union]]
   [clojure.string :refer [join]]
  ;;  ["@fullcalendar/interaction" :default interactionPlugin]
  ;;  ["@fullcalendar/list" :default listPlugin]
  ;;  ["@fullcalendar/react" :default FullCalendar]
  ;;  ["@fullcalendar/timegrid" :default timeGridPlugin]
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
  (let [provider-id #(.. % -_def -extendedProps -provider_id)]
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

(defn- fulfilled? [appt need]
  (contains? (set (map :need/id (:fulfillments appt))) need))

(defn- needs? [appt need]
  (and (contains? (set (map :need/id (:access-needs appt))) need)
       (not (fulfilled? appt need))))

(defn- needs-any? [appt needs]
  (if (seq needs)
    (let [predicates (map #(fn [appt] (needs? appt %)) needs)]
      (boolean ((apply some-fn predicates) appt)))
    false))

(defn- any-unfulfilled? [appt]
  (let [need-ids (map :need/id (:access-needs appt))]
    (not (every? (partial fulfilled? appt) need-ids))))

(comment
  (any-unfulfilled? {:access-needs #{{:need/id 1} {:need/id 2}}
                     :fulfillments #{{:need/id 1} {:need/id 2}}})
  ;; => false
  (any-unfulfilled? {:access-needs #{{:need/id 1} {:need/id 2}}
                     :fulfillments #{{:need/id 1}}})
  ;; => true
  (any-unfulfilled? {:access-needs #{{:need/id 1} {:need/id 2}}
                     :fulfillments #{{:need/id 42}}})
  ;; => true
  (any-unfulfilled? {:access-needs #{}
                     :fulfillments #{{:need/id 42}}})
  ;; => false

  (def with-needs {:access-needs #{{:need/id 1} {:need/id 2}}})
  (needs? with-needs 1)
  ;; => true
  (needs? with-needs 2)
  ;; => true
  (needs? with-needs 3)
  ;; => false
  (needs-any? with-needs [1])
  ;; => true
  (needs-any? with-needs [1 2])
  ;; => true
  (needs-any? with-needs [1 3])
  ;; => true
  (needs-any? with-needs [3 4])
  ;; => false

  (def fulfilled {:access-needs #{{:need/id 1} {:need/id 2}}
                  :fulfillments #{{:need/id 1}}})
  (fulfilled? fulfilled 1)
  ;; => true
  (needs? fulfilled 1)
  ;; => false
  (needs? fulfilled 2)
  ;; => true
  (needs? fulfilled 3)
  ;; => false

  (needs-any? fulfilled [1])
  ;; => false
  (needs-any? fulfilled [2])
  ;; => true
  (needs-any? fulfilled [1 2])
  ;; => true
  (needs-any? fulfilled [3])
  ;; => false

  ;;
  )

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

;; (rf/reg-sub ::focused-appointment :focused-appointment)
(rf/reg-sub ::focused-appointment (fn [{:keys [appointments focused-appointment]}]
                                     (some->> focused-appointment
                                              (get appointments))))
(rf/reg-sub ::events visible-events)
(rf/reg-sub ::filters :filters)
(rf/reg-sub ::providers providers)
(rf/reg-sub ::access-needs access-needs)
(rf/reg-sub ::access-need (fn [{:keys [needs]} [_ id]]
                            (get needs id)))
(rf/reg-sub ::access-needs-filter-summary access-needs-filter-summary)
(rf/reg-sub ::can-view-medical-needs? (fn [{:keys [user-id users]}]
                                        (let [user (get users user-id)]
                                          (contains? (:roles user) :doc))))

(comment
  @(rf/subscribe [::focused-appointment])
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
               ::new-availability)))

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

(rf/reg-event-db ::focus-appointment (fn [db [_ id]]
                                       (assoc db :focused-appointment id)))

;; Keep our FullCalendar instance around so we can re-render on demand.
(def !calendar (r/atom nil))

(defn render-calendar! [_]
  (.refetchEvents @!calendar))

(rf/reg-fx ::render-calendar render-calendar!)

(rf/reg-event-fx ::update-filter (fn [{:keys [db]} dispatch]
                                   {:db (update-filter db dispatch)
                                    ::render-calendar nil}))

(rf/reg-event-fx ::clear-filter (fn [{:keys [db]} filter-key]
                                  {:db (clear-filter db filter-key)
                                   ::render-calendar nil}))

;; Dispatched once the schedule data is ready
(rf/reg-event-fx
 :calendar/load
 (fn [{:keys [db]} [_ {:keys [data errors]}]]
   (if (seq errors)
     ;; TODO some kind of real error handling
     (do (prn errors) {:db db})
     {:db (-> db
              (assoc
               :users          (:users data)
               :my-invitations (:invitations data)
               :availabilities (:availabilities data)
               :appointments   (:appointments data))
              (assoc-in
               [:filters :providers]
               (union (set (map :user/id (vals (:availabilies data))))
                      (set (map :user/id (vals (:appointments data)))))))
      ::render-calendar nil})))

(rf/reg-event-fx
 ::new-availability
 (fn [{:keys [db]} [_ {:keys [data]}]]
   (let [avail (:availability data)]
     {:db (assoc-in db [:availabilities (:id avail)] avail)
      ::render-calendar nil})))

(rf/reg-event-fx
 ::availability-updated
 (fn [{:keys [db]} [_ {:keys [data]}]]
   (let [avail (:availability data)]
     {:db (update-availability db avail)
      ::render-calendar nil})))

(rf/reg-event-fx
 ::availability-deleted
 (fn [{:keys [db]} [_ {:keys [data]}]]
   (let [avail (:availability data)]
     {:db (delete-availability db (:id avail))
      ::render-calendar nil})))

(comment
  @(rf/subscribe [::filters])
  @(rf/subscribe [::providers])
  @(rf/subscribe [::access-needs-filter-summary])

  (rf/dispatch [::update-filter :providers 3])
  (rf/dispatch [::update-filter :providers 4]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;         Components        ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
                      [:div.filter-field
                       [:input {:id html-id
                                :type :checkbox
                                :on-change #(rf/dispatch [::update-filter :access-needs id])
                                :checked (contains? (:access-needs filters) id)
                                :disabled (not showing-appointments?)
                                :style {}}]
                       [:label.filter-label {:for html-id}
                        (str "Needs " name)]]))
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

(defn appointment-details []
  (let [appt @(rf/subscribe [::focused-appointment])
        {:keys [pronouns email phone ok_to_text reason access-needs]} appt
        start (moment. (:start appt))
        can-view-medical-needs? @(rf/subscribe [::can-view-medical-needs?])]
    [:article.appointment
     [:header
      [:h2.appointment-name (full-name appt) (when (seq pronouns)
                                               (str " (" pronouns ")"))]
      [:h3 (.format start "h:mma ddd, MMM D")]]
     [:div.appointment-details
      [:div.appointment-field-group
       [:h3 "Contact"]
       [:dl
        (when (seq email)
          [:<>
           [:dt "Email"]
           [:dd [:a {:href (str "mailto:")} email]]])
        (when (seq phone)
          [:<>
           [:dt "Phone"]
           [:dd [:a {:href (str "tel:" phone)} phone]]])
        [:dt "OK to text?"]
        [:dd (if ok_to_text "yes" "no")]]]
      [:div.appointment-field-group
       [:h3 "Access Needs"]
       (if (seq access-needs)
         [:<>
          [:div.access-needs-indicator.--unmet "Unmet access needs"]
          (doall (map (fn [{:need/keys [id] :as appt-need}]
                        (let [need (merge @(rf/subscribe [::access-need id]) appt-need)]
                          [:p (access-need need)]))
                      access-needs))]
         [:<>
          [:div.access-needs-indicator.--met "Access needs met!"]])]]
     (when can-view-medical-needs?
       [:div.appointment-field-group
        [:h3 "Medical Needs"]
        [:p reason]])]))

(defn calendar []
  (let [!ref (atom nil)]
    (r/create-class
     {:display-name "FullCalendar"
      :reagent-render
      (fn []
        [:div.full-calendar {:ref #(reset! !ref %)}])
      :component-did-mount
      (fn []
        (let [events-fn (fn [_info on-success _on-error]
                          (on-success (clj->js @(rf/subscribe [::events]))))
              cal (js/FullCalendar.Calendar.
                   @!ref
                   #js {:selectable true
                        :headerToolbar #js {:start "today prev next"
                                            :center "title"
                                            :end "dayGridMonth timeGridWeek listWeek"}
                        :eventClick (fn [info]
                                      (let [e (.-event info)
                                            id (js/parseInt (.-id e))]
                                        (when (appointment? e)
                                          (rf/dispatch [::focus-appointment id]))))
                        :eventDidMount (fn [info]
                                         (when (.. info -event -_def -extendedProps -deletable)
                                           (let [id (.. info -event -id)
                                                 elem (.-el info)
                                                 delete-btn (js/document.createElement "i")
                                                 on-click #(rf/dispatch [::delete-availability id])]
                                             (.addEventListener delete-btn "click" on-click)
                                             (set! (.-innerText delete-btn) "×")
                                             (.add (.-classList delete-btn) "rtc-delete")
                                             (.appendChild elem delete-btn))))
                        :eventChange (fn [info]
                                       (let [e (.-event info)
                                             id (js/parseInt (.-id e))]
                                         (rf/dispatch [::update-availability {:id id
                                                                              :start (.-start e)
                                                                              :end (.-end e)}])))
                        :select (fn [event]
                                  (rf/dispatch [::create-availability {:start (.-start event)
                                                                            :end   (.-end event)}]))
                        :eventOverlap can-overlap?
                        :events events-fn
                        :initialView "timeGridWeek"})]
          ;; Save our FullCalendar.Calendar instance in an atom
          (reset! !calendar cal)
          (.render cal)))})))


(defn care-schedule []
  ;; TODO when this issue gets resolved see if we can use FullCalendar component:
  ;; https://github.com/fullcalendar/fullcalendar/issues/5393
  #_[:> FullCalendar {;:header-toolbar #js {:left "prev,next today" :center "title" :right "dayGrid"}
                      :selectable true
                      :select (fn [info]
                                (js/console.log info))
                      :date-click (fn [info]
                                    (js/console.log info))
                      :default-view "timeGridWeek"
                      :events (*generate-calendar-events 50)
                    ;; TODO enable drag & click interaction
                      :plugins [#_interactionPlugin listPlugin timeGridPlugin]}]
  (let [appt @(rf/subscribe [::focused-appointment])]
    [:div.schedule-container
     (when appt
       [modal
        [appointment-details]])
     [:div.care-schedule
      [filter-controls]
      [calendar]]]))