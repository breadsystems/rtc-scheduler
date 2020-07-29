(ns rtc.admin.calendar
  (:require
   [clojure.string :refer [join]]
  ;;  ["@fullcalendar/interaction" :default interactionPlugin]
  ;;  ["@fullcalendar/list" :default listPlugin]
  ;;  ["@fullcalendar/react" :default FullCalendar]
  ;;  ["@fullcalendar/timegrid" :default timeGridPlugin]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [rtc.api.core :as api]))



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
  (not (and (= "availability" (.. a -extendedProps -type))
            (= "availability" (.. b -extendedProps -type)))))

(defn update-availability [db [_ id avail-data]]
  (update-in db [:availabilities id] merge avail-data))

(defn delete-availability [db [_ id]]
  (update db :availabilities dissoc (js/parseInt id)))

(defn current-user [{:keys [users user-id]}]
  (get users user-id))

(defn user->name [{:keys [first_name last_name]}]
  (str first_name " " last_name))

(defn- by-provider [events providers]
  (filter #(contains? providers (:user/id %)) events))

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

(comment
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

(defn- deletable-for? [avail user-id]
  (= user-id (:user/id avail)))

(defmulti ->fc-event :event/type)

(defmethod ->fc-event :default [e] e)

(defmethod ->fc-event :availability [event]
  (assoc event
         :title (full-name (:event/provider event))
         :editable true
         :backgroundColor "#325685"
         :classNames ["rtc-availability"]))

(defmethod ->fc-event :appointment [appt]
  (assoc appt
         :title (:name appt)
         :editable false
         :classNames ["rtc-appointment"]))

(defn visible-events [{:keys [availabilities appointments filters users needs user-id]}]
  (let [{:keys [availabilities? appointments? providers access-needs]} filters
        ;; Only type and provider filters apply to Availabilities.
        visible-avails (-> (when availabilities?
                             (map #(assoc %
                                          :event/type :availability
                                          :deletable (deletable-for? % user-id))
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
                               :event/provider (get users (:user/id event))
                               :event/needs (map #(get needs (:need/id %)) (:access-needs event))))
                      (concat visible-avails visible-appts))]
    (map ->fc-event enriched)))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;      Subscriptions        ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(rf/reg-sub ::my-availabilities (fn [{:keys [availabilities user-id]}]
                                  (filter-by-id (vals availabilities) user-id)))

(rf/reg-sub ::my-appointments (fn [{:keys [appointments user-id]}]
                                (filter-by-id (vals appointments) user-id)))

(rf/reg-sub ::focused-appointment :focused-appointment)
(rf/reg-sub ::events visible-events)
(rf/reg-sub ::filters identity)



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

(rf/reg-event-fx ::create-availability (fn [{:keys [db]} [_ avail]]
                                         ;; TODO do id and overlap check server-side
                                         (let [{:keys [availabilities user-id]} db
                                               id (inc (apply max (keys availabilities)))
                                               avail (merge avail {:availability/id id
                                                                   :event/type :availability
                                                                   :user/id user-id})]
                                           (if (overlaps-any? avail (vals availabilities))
                                             ;; Overlap in availability is not allowed!
                                             {:db db}
                                             ;; No overlaps; update db
                                             {:db (assoc-in db [:availabilities id] avail)
                                              ::render-calendar nil}))))

(rf/reg-event-fx ::update-availability (fn [{:keys [db]} dispatch]
                                         {:db (update-availability db dispatch)
                                          ::render-calendar nil}))

(rf/reg-event-fx ::delete-availability (fn [{:keys [db]} dispatch]
                                         {:db (delete-availability db dispatch)
                                          ::render-calendar nil}))

(rf/reg-event-db ::focus-appointment (fn [db [_ appt]]
                                       (assoc db :focused-appointment appt)))

;; Keep our FullCalendar instance around so we can re-render on demand.
(def !calendar (r/atom nil))

(rf/reg-fx ::render-calendar (fn [_]
                               (.refetchEvents @!calendar)))

(rf/reg-event-fx ::update-filter (fn [{:keys [db]} dispatch]
                                   {:db (update-filter db dispatch)
                                    ::render-calendar nil}))

(comment
  @(rf/subscribe [::my-availabilities])
  @(rf/subscribe [::my-appointments])
  @(rf/subscribe [::focused-appointment])
  @(rf/subscribe [::filters])

  (rf/dispatch [::create-availability {:db {:start "2020-07-31T10:00"
                                            :end "2020-07-31T17:00"
                                            :event/type :availability
                                            :user/id 3}}])

  (rf/dispatch [::update-filter :providers 3]))



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
                        :editable true
                        :headerToolbar #js {:start "today prev next"
                                            :center "title"
                                            :end "dayGridMonth timeGridWeek listWeek"}
                        :eventClick (fn [info]
                                      (let [e (.-event info)]
                                        (when (appointment? e)
                                          (rf/dispatch [::focus-appointment e]))))
                        :eventDidMount (fn [info]
                                         (when (.. info -event -_def -extendedProps -deletable)
                                           (let [id (.. info -event -id)
                                                 elem (.-el info)
                                                 delete-btn (js/document.createElement "i")
                                                 on-click #(rf/dispatch-sync [::delete-availability id])]
                                             (.addEventListener delete-btn "click" on-click)
                                             (set! (.-innerText delete-btn) "×")
                                             (.add (.-classList delete-btn) "rtc-delete")
                                             (.appendChild elem delete-btn))))
                        :eventChange (fn [info]
                                       (let [e (.-event info)
                                             id (js/parseInt (.-id e))]
                                         (rf/dispatch-sync [::update-availability id {:start (.-start e)
                                                                                      :end (.-end e)}])))
                        :select (fn [event]
                                  (rf/dispatch-sync [::create-availability {:start (.-start event)
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
    [:div.calendar-container
     (when appt
       [modal [:div "heyyy"]])
     [calendar]]))