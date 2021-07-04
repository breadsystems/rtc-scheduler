(ns rtc.notifier.core
  (:require
    [mount.core :refer [defstate]]
    [rtc.notifier.twilio :as twilio]
    [rtc.event :as e]
    [rtc.providers.core :as provider]
    [rtc.util :refer [->zoned format-zoned]]))

(defn appointment->sms [{:keys [phone
                                provider_first_name
                                provider_last_name
                                start_time]}]
  (let [pacific (->zoned start_time "America/Los_Angeles")
        eastern (->zoned start_time "America/New_York")]
    {:to (twilio/us-phone phone)
     :message (format
                ;; TODO i18n
                "Your appointment at %s / %s with %s is confirmed."
                (format-zoned pacific "h:mma z")
                (format-zoned eastern "h:mma z EEE, MMM d")
                (str provider_first_name " " provider_last_name))}))

(defn send-sms? [appt]
  (boolean (and (= 1 (:text-ok appt)) (seq (:phone appt)))))

(defn appointment->provider-sms
  "Returns an SMS map of the form {:to ... :message ...} given a provider with
  a phone and a start_time inst. Returns nil if either the provider phone is
  empty OR the start_time is not an inst."
  [{:keys [provider start_time]}]
  (when (and (:phone provider) (inst? start_time))
    (let [pacific (->zoned start_time "America/Los_Angeles")
          eastern (->zoned start_time "America/New_York")]
      {:to (twilio/us-phone (:phone provider))
       :message (format
                  ;; TODO i18n
                  "Someone booked an appointment with you at %s / %s."
                  (format-zoned pacific "h:mma z")
                  (format-zoned eastern "h:mma z EEE, MMM d"))})))

(defn- appt->provider [{:keys [provider_id]}]
  (provider/id->provider provider_id))

(defn- booked-appointment! [appt]
  (twilio/send-sms! (appointment->provider-sms
                      (assoc appt :provider (appt->provider appt))))
  (when (send-sms? appt)
    (twilio/send-sms! (appointment->sms appt))))

(defonce unsub-notifiers (atom nil))

(defstate appointment-notifiers
  :start (let [unsub! (e/subscribe!
                        :booked-appointment
                        (fn [{appt :event/appointment}]
                          (booked-appointment! appt)))]
           (reset! unsub-notifiers unsub!))
  :stop (when-let [unsub! @unsub-notifiers]
          (unsub!)))

(comment
  (def $appt
    {:provider_first_name "Doctor",
     :phone "253 222 9139",
     :provider_last_name "Someone",
     :name "Coby",
     :start_time #inst "2021-07-10T01:30:00.000000000-00:00",
     :pronouns nil,
     :start #inst "2021-07-10T01:30:00.000-00:00",
     :description-of-needs "asdf",
     :state "WA",
     :end_time #inst "2021-07-10T02:00:00.000000000-00:00",
     :id 25,
     :provider_id 6
     :end #inst "2021-07-10T02:00:00.000-00:00",
     :text-ok 1,
     :preferred-communication-method "phone"})

  (:phone (appt->provider $appt))

  (nil? (appointment->provider-sms $appt))

  (appointment->provider-sms
    (assoc $appt :provider (appt->provider $appt)))

  (booked-appointment! $appt)
  (booked-appointment! (assoc $appt :text-ok nil))

  (e/publish! {:event/type :booked-appointment
               :event/appointment {:my :appt}}))
