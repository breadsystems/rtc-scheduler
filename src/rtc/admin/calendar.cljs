(ns rtc.admin.calendar
  (:require
  ;;  ["@fullcalendar/interaction" :default interactionPlugin]
  ;;  ["@fullcalendar/list" :default listPlugin]
  ;;  ["@fullcalendar/react" :default FullCalendar]
  ;;  ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["moment" :as moment]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [rtc.api.core :as api]))



(rf/reg-sub ::my-availabilities (fn [{:keys [availabilities]}]
                                  (conj availabilities
                                        {:start "2020-07-27T09:00"
                                         :end "2020-07-27T16:00"
                                         :event/type :availability}
                                        {:start "2020-07-29T09:00"
                                         :end "2020-07-29T16:00"
                                         :event/type :availability}
                                        {:start "2020-07-30T10:00"
                                         :end "2020-07-30T15:00"
                                         :event/type :availability}
                                        {:start "2020-08-01T11:00"
                                         :end "2020-08-01T15:00"
                                         :event/type :availability})))


(defn filter-my-appointments [{:keys [appointments]}]
  (conj appointments {:start "2020-08-01T12:00"
                      :end "2020-08-01T12:30"
                      :event/type :appointment}))

;; (api/query! )
(rf/reg-sub ::my-appointments filter-my-appointments)

(comment
  @(rf/subscribe [::my-appointments]))


(rf/reg-fx ::render-calendar (fn [[full-calendar]]
                               (.refetchEvents full-calendar)
                               (.render full-calendar)))

(rf/reg-event-db ::create-availability (fn [db [_ fc-event]]
                                         (let [avail {:start (.-start fc-event)
                                                      :end (.-end fc-event)
                                                      :event/type :availability}]
                                           (update db :availabilities conj avail))))

(comment
  (rf/dispatch [::create-availability {:start "2020-07-31T10:00"
                                       :end "2020-07-31T17:00"
                                       :event/type :availability}]))


(defmulti ->fc-event :event/type)
(defmethod ->fc-event :default [e] e)
(defmethod ->fc-event :availability [event]
  (assoc event
         :editable true
         :backgroundColor "#325685"))
(defmethod ->fc-event :appointment [event]
  (assoc event
         :editable false))

(defn can-overlap? [a b]
  (not (and (= "availability" (.. a -extendedProps -type))
            (= "availability" (.. b -extendedProps -type)))))


(defn calendar []
  (let [!ref (atom nil)
        !calendar (atom nil)]
    (r/create-class
     {:display-name "FullCalendar"
      :reagent-render
      (fn []
        [:div.full-calendar {:ref #(reset! !ref %)}])
      :component-did-mount
      (fn []
        (when @!ref
          (let [events-fn (fn [_info on-success _on-error]
                            (on-success
                             (clj->js
                              (map ->fc-event
                                   (concat @(rf/subscribe [::my-appointments])
                                           @(rf/subscribe [::my-availabilities]))))))
                cal (js/FullCalendar.Calendar.
                     @!ref
                     #js {:selectable true
                          :editable true
                          :select (fn [info]
                                    (rf/dispatch-sync [::create-availability info])
                                    (.refetchEvents @!calendar)
                                    (.render @!calendar))
                          :eventOverlap can-overlap?
                          :events events-fn
                          :initialView "timeGridWeek"})]
            ;; Save our FullCalendar.Calendar instance in an atom
            (reset! !calendar cal)
            (.render cal))))})))


(defn availability-schedule []
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
  [calendar])