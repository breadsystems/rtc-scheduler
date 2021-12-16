(ns rtc.notifier.api
  (:require
    [rtc.notifier.twilio :as twilio]
    [rtc.notifier.sendgrid :as sendgrid]))

(defmulti notify! :notification/type)

(defmethod notify! :sms [{:keys [to message] :as sms}]
  (when (and to message)
    (twilio/send-sms! sms)))

(defmethod notify! :email [{:keys [to message] :as email}]
  (when (and to message)
    (sendgrid/send-email! email)))
