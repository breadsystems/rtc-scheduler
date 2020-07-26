(ns rtc.admin.calendar
  (:require
  ;;  ["@fullcalendar/interaction" :default interactionPlugin]
   ["@fullcalendar/list" :default listPlugin]
   ["@fullcalendar/react" :default FullCalendar]
   ["@fullcalendar/timegrid" :default timeGridPlugin]
   ["moment" :as moment]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [rtc.api.core :as api]))


;; Dispatched on initial page load
(defn- *generate-calendar-events [cnt]
  (doall (distinct (map (fn [_]
                          (let [m (doto (moment)
                                    (.add (rand-int 3) "hours")
                                    (.subtract (rand-int 6) "hours")
                                    (.add (inc (rand-int 20)) "days"))
                                start (.format m "YYYY-MM-DDTHH:00")
                                end   (.format m "YYYY-MM-DDTHH:30")]
                            {:start start
                             :end end
                             :allDay false
                             :provider_id 123}))
                        (range 0 cnt)))))


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
          (let [cal (js/FullCalendar.Calendar.
                     @!ref
                     #js {:selectable true
                          :select (fn [info]
                                    (js/console.log info))
                          :dateClick (fn [info]
                                       (js/console.log info))
                          :events (*generate-calendar-events 50)
                          :initialView "timeGridWeek"})]

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