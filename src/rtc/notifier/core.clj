(ns rtc.notifier.core
  (:require
    [mount.core :refer [defstate]]
    [rtc.notifier.twilio :as twilio]
    [rtc.notifier.sendgrid :as sendgrid]
    [rtc.event :as e]
    [rtc.providers.core :as provider]
    [rtc.util :refer [->zoned format-zoned]]))

(defn- coast-times [dt]
  (let [pacific (->zoned dt "America/Los_Angeles")
        eastern (->zoned dt "America/New_York")]
    (format "%s / %s"
            (format-zoned pacific "h:mma z")
            (format-zoned eastern "h:mma z EEE, MMM d"))))

;;
;; SMS NOTIFICATIONS
;;

(defn appointment->sms [{:keys [phone
                                provider_first_name
                                provider_last_name
                                start_time]}]
  {:to (twilio/us-phone phone)
   :message (format
              ;; TODO i18n
              "Your appointment at %s with %s is confirmed."
              (coast-times start_time)
              (str provider_first_name " " provider_last_name))})

(defn send-sms? [appt]
  (boolean (and (= 1 (:text-ok appt)) (seq (:phone appt)))))

(defn appointment->provider-sms
  "Returns an SMS map of the form {:to ... :message ...} given a provider with
  a phone and a start_time inst. Returns nil if either the provider phone is
  empty OR the start_time is not an inst."
  [{:keys [provider start_time]}]
  (when (and (:phone provider) (inst? start_time))
    {:to (twilio/us-phone (:phone provider))
     :message (format
                ;; TODO i18n
                "Someone booked an appointment with you at %s."
                (coast-times start_time))}))

;;
;; EMAIL NOTIFICATIONS
;;

(defn send-email? [appt]
  (boolean (seq (:email appt))))

(defn appointment->email
  "Returns an email map of the form {:to ... :to-name ... :message ...}
  given an email, provider first/last name, and a start_time inst. Returns nil
  if email, provider_first_name, provider_last_name, start_time are empty
  OR the start_time is not an inst."
  [{:keys [email
           name
           provider_first_name
           provider_last_name
           start_time]}]
  (when (and email
             provider_first_name
             provider_last_name
             (inst? start_time))
    {:to email
     :to-name name
     :subject "Your appointment with the Radical Telehealth Collective"
     :message  (format
                 ;; TODO i18n
                 (str "Your appointment at %s with %s is confirmed."
                      " Thank you for booking your appointment with the"
                      " Radical Telehealth Collective.")
                 (coast-times start_time)
                 (str provider_first_name " " provider_last_name))
     }))

(defn appointment->provider-email
  "Returns an email map of the form {:to ... :to-name ... :message ...}
  given an email, recipient name, and a start_time inst. Returns nil if
  either the email is empty OR the start_time is not an inst."
  [{:keys [provider start_time]}]
  (when (and (:email provider) (inst? start_time))
    {:to (:email provider)
     :subject "New RTC Appointment"
     :message (format
                ;; TODO i18n
                (str "Someone booked an appointment with you at %s."
                     " Go to %s for details.")
                (coast-times start_time)
                ;; TODO env config?
                "https://www.radicaltelehealthcollective.org/comrades"
                )}))

;;
;; EVENT HANDLERS AND HELPERS
;;

(defn- appt->provider [{:keys [provider_id]}]
  (provider/id->provider provider_id))

(defn- booked-appointment! [appt]
  ;; Notify the careseeker, honoring their consent.
  (when (send-sms? appt)
    (twilio/send-sms! (appointment->sms appt)))
  (sendgrid/send-email! appt)

  ;; Notify the provider.
  (let [appt (assoc appt :provider (appt->provider appt))]
    ;; TODO text notification preferences
    (twilio/send-sms! (appointment->provider-sms appt))
    (sendgrid/send-email! (appointment->provider-email appt))))

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
  (def $provider (provider/email->provider "ctamayo+test@protonmail.com"))

  (def $appt
    {:name "Coby Test"
     :email "coby@tamayo.email"
     :phone "253 222 9139"
     :provider_id (:id $provider)
     :provider_first_name "Doctor"
     :provider_last_name "Someone"
     :start_time #inst "2021-07-10T01:30:00.000000000-00:00"
     :pronouns nil
     :start #inst "2021-07-10T01:30:00.000-00:00"
     :description-of-needs "asdf"
     :state "WA"
     :end_time #inst "2021-07-10T02:00:00.000000000-00:00"
     :id 25
     :end #inst "2021-07-10T02:00:00.000-00:00"
     :text-ok 1
     :preferred-communication-method "phone"})

  (:phone (appt->provider $appt))

  ;; No :provider yet; need an extra assoc.
  (nil? (appointment->provider-sms $appt))
  (nil? (appointment->provider-email $appt))

  ;; Full valid appt we can send to our various notifier fns.
  (def $with-provider (assoc $appt :provider (appt->provider $appt)))

  (twilio/send-sms! (appointment->sms $with-provider))
  (twilio/send-sms! (appointment->provider-sms $with-provider))
  (sendgrid/send-email! (appointment->email $with-provider))
  (sendgrid/send-email! (appointment->provider-email $with-provider))

  (booked-appointment! $with-provider)
  (booked-appointment! (assoc $with-provider :text-ok nil))

  ;; Send an appointment through the generic pub/sub stream. This is what
  ;; actually sends notifications (and whatever any other subscribers are
  ;; doing, which at time of writing is nothing :D).
  (e/publish! {:event/type :booked-appointment
               :event/appointment $with-provider}))
