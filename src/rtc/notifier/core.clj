(ns rtc.notifier.core
  (:require
    [mount.core :refer [defstate]]
    [rtc.event :as e]
    [rtc.notifier.invites :as invt]
    [rtc.notifier.appointments :as appt]))

;;
;; APPOINTMENT REQUEST NOTIFICATIONS
;;

(defonce unsub-appointment-requests (atom nil))

(defstate appointment-request-notifiers
  :start (let [unsub! (e/subscribe!
                        :requested-appointment
                        (fn [{appt-req :event/request}]
                          (appt/requested-appointment! appt-req)))]
           (reset! unsub-appointment-requests unsub!))
  :stop (when-let [unsub! @unsub-appointment-requests]
          (unsub!)))

;;
;; APPOINTMENT NOTIFICATIONS
;;

(defonce unsub-appointments (atom nil))

(defstate appointment-notifiers
  :start (let [unsub! (e/subscribe!
                        :booked-appointment
                        (fn [{appt :event/appointment}]
                          (appt/booked-appointment! appt)))]
           (reset! unsub-appointments unsub!))
  :stop (when-let [unsub! @unsub-appointments]
          (unsub!)))

;;
;; INVITE NOTIFICATIONS
;;

(defonce unsub-invites (atom nil))

(defstate invite-notifiers
  :start (let [unsub! (e/subscribe!
                        :invited
                        (fn [{invite :event/invitation}]
                          (invt/invited! invite)))]
           (reset! unsub-invites unsub!))
  :stop (when-let [unsub! @unsub-invites]
          (unsub!)))

(comment

  ;; Send an appointment through the generic pub/sub stream. This is what
  ;; actually sends notifications (and whatever any other subscribers are
  ;; doing, which at time of writing is nothing :D).
  (e/publish! {:event/type :booked-appointment
               :event/appointment $with-provider}))
