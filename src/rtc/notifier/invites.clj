(ns rtc.notifier.invites
  (:require
    [rtc.notifier.sendgrid :as sendgrid]))

(defn invited! [{:keys [email code]}]
  ;; TODO
  (prn email code))
