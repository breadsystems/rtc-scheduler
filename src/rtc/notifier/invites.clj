(ns rtc.notifier.invites
  (:require
    [rtc.notifier.sendgrid :as sendgrid]))

(defn- invite->email [{:keys [email url]}]
  (let [message
        (format (str "Welcome to the Radical Telehealth Collective!\n\n"
                     "Go here to redeem your invite"
                     " and set up your account: %s\n\n")
                url)]
    {:subject "Your Radical Telehealth Collective invite"
     :to email
     :message message}))

(defn invited! [invite]
  (sendgrid/send-email! (invite->email invite)))
