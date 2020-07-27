(ns rtc.admin.calendar
  (:require
  ;;  ["@fullcalendar/interaction" :default interactionPlugin]
  ;;  ["@fullcalendar/list" :default listPlugin]
  ;;  ["@fullcalendar/react" :default FullCalendar]
  ;;  ["@fullcalendar/timegrid" :default timeGridPlugin]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [rtc.api.core :as api]
   [rtc.admin.events :as e]))



    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;                           ;;
  ;;    Availability Logic     ;;
 ;;                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Core logic for availabilities
;;

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


(rf/reg-sub ::my-availabilities (fn [{:keys [availabilities user]}]
                                  (filter-by-id availabilities (:id user))))

(rf/reg-sub ::my-appointments (fn [{:keys [appointments user]}]
                                (filter-by-id appointments (:id user))))

(comment
  @(rf/subscribe [::my-availabilities])
  @(rf/subscribe [::my-appointments]))


(rf/reg-fx ::render-calendar (fn [[full-calendar]]
                               (.refetchEvents full-calendar)
                               (.render full-calendar)))

(rf/reg-event-db ::create-availability (fn [{:keys [availabilities user] :as db} [_ fc-event]]
                                         (let [avail {:start (.-start fc-event)
                                                      :end (.-end fc-event)
                                                      :event/type :availability
                                                      :user/id (:id user)}]
                                           (if (overlaps-any? avail availabilities)
                                             ;; Overlap in availability is not allowed!
                                             db
                                             ;; No overlaps; update db
                                             (update db :availabilities conj avail)))))

(comment
  (rf/dispatch [::create-availability {:start "2020-07-31T10:00"
                                       :end "2020-07-31T17:00"
                                       :event/type :availability
                                       :user/id 3}]))


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
                              (map e/->fc-event
                                   (concat @(rf/subscribe [::my-appointments])
                                           @(rf/subscribe [::my-availabilities]))))))
                cal (js/FullCalendar.Calendar.
                     @!ref
                     ;; TODO:
                     ;; * update availability on drag
                     ;; * deal with overlap on create
                     ;; * DELETE
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