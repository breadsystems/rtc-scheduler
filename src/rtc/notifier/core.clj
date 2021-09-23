(ns rtc.notifier.core
  (:require
    [mount.core :refer [defstate]]
    [rtc.event :as e]
    [rtc.notifier.appointments :as appt]))

(defonce unsub-appointments (atom nil))

(defstate appointment-notifiers
  :start (let [unsub! (e/subscribe!
                        :booked-appointment
                        (fn [{appt :event/appointment}]
                          (appt/booked-appointment! appt)))]
           (reset! unsub-appointments unsub!))
  :stop (when-let [unsub! @unsub-appointments]
          (unsub!)))

(comment

  ;; Send an appointment through the generic pub/sub stream. This is what
  ;; actually sends notifications (and whatever any other subscribers are
  ;; doing, which at time of writing is nothing :D).
  (e/publish! {:event/type :booked-appointment
               :event/appointment $with-provider}))
