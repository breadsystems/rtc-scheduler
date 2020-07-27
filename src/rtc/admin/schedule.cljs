(ns rtc.admin.schedule
  (:require
  ;;  ["@fullcalendar/interaction" :default interactionPlugin]
  ;;  ["@fullcalendar/list" :default listPlugin]
  ;;  ["@fullcalendar/react" :default FullCalendar]
  ;;  ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["moment" :as moment]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [rtc.api.core :as api]
   [rtc.admin.events :as e]))


(rf/reg-sub ::all-appointments :appointments)
(rf/reg-sub ::all-availabilities :availabilities)


(defn calendar []
  (let [!ref (atom nil)]
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
                              (map e/->fc-event @(rf/subscribe [::all-appointments])))))
                cal (js/FullCalendar.Calendar.
                     @!ref
                     #js {:selectable true
                          :select (fn [info]
                                    (js/console.log info))
                          :dateClick (fn [info]
                                       (js/console.log info))
                          :events events-fn
                          :initialView "listWeek"})]

            (.render cal))))})))


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
  [calendar])

(comment
  (js/console.log #js {:left "prev,next today" :center "title" :right "dayGrid"}))