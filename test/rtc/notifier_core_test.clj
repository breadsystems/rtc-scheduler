(ns rtc.notifier-core-test
  (:require
    [clojure.test :refer [are deftest is]]
    [rtc.notifier.core :as notify]))

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

(deftest test-send-sms?

  (is (true? (notify/send-sms? {:text-ok 1 :phone "1234567890"})))
  (is (false? (notify/send-sms? {:text-ok nil :phone "1234567890"})))
  (is (false? (notify/send-sms? {:phone "1234567890"})))
  (is (false? (notify/send-sms? {:phone ""})))
  (is (false? (notify/send-sms? {:text-ok 1})))
  (is (false? (notify/send-sms? {:text-ok 321})))
  (is (false? (notify/send-sms? {:text-ok false :phone "123467890"}))))
