(ns rtc.notifier-core-test
  (:require
    [clojure.test :refer [are deftest is]]
    [kaocha.repl :as k]
    [rtc.notifier.core :as notify]))

;;
;; SMS NOTIFICATIONS
;;

(deftest test-appointment->sms

  (are
    [sms appt]
    (= sms (notify/appointment->sms appt))

    {:to "+12535551234"
     :message "Your appointment at 5:30PM PDT / 8:30PM EDT Fri, Jul 9 with Ursula Le Guin is confirmed."}
    {:phone "253 555 1234"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    {:to "+12535551234"
     :message "Your appointment at 5:30PM PDT / 8:30PM EDT Fri, Jul 9 with Ursula Le Guin is confirmed."}
    {:phone "1 253 555 1234"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    {:to "+12535551234"
     :message "Your appointment at 5:30PM PDT / 8:30PM EDT Fri, Jul 9 with Ursula Le Guin is confirmed."}
    {:phone "+1 253 555 1234"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    {:to "+12535551234"
     :message "Your appointment at 4:30PM PST / 7:30PM EST Tue, Mar 9 with Ursula Le Guin is confirmed."}
    {:phone "+12535551234"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-03-10T00:30:00.000000000-00:00"}))

(deftest test-appointment->provider-sms

  (are
    [sms appt]
    (= sms (notify/appointment->provider-sms appt))

    nil {}
    nil {:provider nil}
    nil {:provider {}} ;; no phone
    nil {:start_time nil :provider {:phone "1234567890"}}
    nil {:start_time "xyz" :provider {:phone "1234567890"}}
    nil {:start_time "2021-03-10T00:33:00.000000000-00:00" ;; not an inst
         :provider {:phone "1234567890"}}

    {:to "+12535551234"
     :message "Someone booked an appointment with you at 4:33PM PST / 7:33PM EST Tue, Mar 9."}
    {:provider {:phone "+12535551234"}
     :start_time #inst "2021-03-10T00:33:00.000000000-00:00"}

    {:to "+12535551234"
     :message "Someone booked an appointment with you at 4:30PM PST / 7:30PM EST Tue, Mar 9."}
    {:provider {:phone "+12535551234"}
     :start_time #inst "2021-03-10T00:30:00.000000000-00:00"}

    {:to "+12535550987"
     :message "Someone booked an appointment with you at 4:30PM PST / 7:30PM EST Tue, Mar 9."}
    {:provider {:phone "+1 253 555 0987"}
     :start_time #inst "2021-03-10T00:30:00.000000000-00:00"}
    ))

(deftest test-send-sms?

  (is (true? (notify/send-sms? {:text-ok 1 :phone "1234567890"})))
  (is (false? (notify/send-sms? {:text-ok nil :phone "1234567890"})))
  (is (false? (notify/send-sms? {:phone "1234567890"})))
  (is (false? (notify/send-sms? {:phone ""})))
  (is (false? (notify/send-sms? {:text-ok 1})))
  (is (false? (notify/send-sms? {:text-ok 321})))
  (is (false? (notify/send-sms? {:text-ok false :phone "123467890"}))))

;;
;; EMAIL NOTIFICATIONS
;;

(comment
  (k/run))

(deftest test-appointment->email

  (are
    [email appt]
    (= email (notify/appointment->email appt))

    {:to "careseeker@example.com"
     :to-name nil
     :subject "Your appointment with the Radical Telehealth Collective"
     :message "Your appointment at 5:30PM PDT / 8:30PM EDT Fri, Jul 9 with Ursula Le Guin is confirmed. Thank you for booking your appointment with the Radical Telehealth Collective."}
    {:email "careseeker@example.com"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    ;; With careseeker name
    {:to "careseeker@example.com"
     :to-name "Shevek"
     :subject "Your appointment with the Radical Telehealth Collective"
     :message "Your appointment at 5:30PM PDT / 8:30PM EDT Fri, Jul 9 with Ursula Le Guin is confirmed. Thank you for booking your appointment with the Radical Telehealth Collective."}
    {:email "careseeker@example.com"
     :name "Shevek"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    nil
    {;; no email
     :name "Shevek"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    nil
    {:email "me@example.com"
     :name "Shevek"
     ;; no provider_first_name
     :provider_last_name "Le Guin"
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    nil
    {:email "me@example.com"
     :name "Shevek"
     :provider_first_name "Ursula"
     ;; no provider_last_name
     :start_time #inst "2021-07-10T00:30:00.000000000-00:00"}

    nil
    {:email "me@example.com"
     :name "Shevek"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     ;; no start_time
     }

    nil
    {:email "me@example.com"
     :name "Shevek"
     :provider_first_name "Ursula"
     :provider_last_name "Le Guin"
     ;; not an inst:
     :start_time "2021-07-10T00:30:00.000000000-00:00"}
    ))

(deftest test-appointment->provider-email

  (are
    [email appt]
    (= email (notify/appointment->provider-email appt))

    nil {}
    nil {:provider nil}
    nil {:provider {}} ;; no email
    nil {:start_time nil :provider {:email "rtc@example.org"}}
    nil {:start_time "xyz" :provider {:email "rtc@example.org"}}
    nil {:start_time "2021-03-10T00:33:00.000000000-00:00" ;; not an inst
         :provider {:email "rtc@example.org"}}

    {:to "rtc@example.org"
     :subject "New RTC Appointment"
     :message "Someone booked an appointment with you at 4:33PM PST / 7:33PM EST Tue, Mar 9. Go to https://www.radicaltelehealthcollective.org/comrades for details."}
    {:provider {:email "rtc@example.org"}
     :start_time #inst "2021-03-10T00:33:00.000000000-00:00"}

    {:to "rtc@example.org"
     :subject "New RTC Appointment"
     :message "Someone booked an appointment with you at 4:30PM PST / 7:30PM EST Tue, Mar 9. Go to https://www.radicaltelehealthcollective.org/comrades for details."}
    {:provider {:email "rtc@example.org"}
     :start_time #inst "2021-03-10T00:30:00.000000000-00:00"}

    {:to "rtc@example.org"
     :subject "New RTC Appointment"
     :message "Someone booked an appointment with you at 4:30PM PST / 7:30PM EST Tue, Mar 9. Go to https://www.radicaltelehealthcollective.org/comrades for details."}
    {:provider {:email "rtc@example.org"}
     :start_time #inst "2021-03-10T00:30:00.000000000-00:00"}
    ))

(deftest test-send-email?

  (is (true? (notify/send-email? {:email "rtc@example.com"})))
  (is (false? (notify/send-email? {})))
  (is (false? (notify/send-email? nil)))
  (is (false? (notify/send-email? {:email ""}))))
