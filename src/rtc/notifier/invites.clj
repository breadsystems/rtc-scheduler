(ns rtc.notifier.invites
  (:require
    [rtc.notifier.sendgrid :as sendgrid]))

(defn- invite->email [{:keys [email url]}]
  {:subject "Your Radical Telehealth Collective invite"
   :to email
   :message
   (format
     (str "Welcome to the Radical Telehealth Collective!\n\n"
          "Go here to redeem your invite and set up your account: %s\n\n")
     url)})

(defn invited! [invite]
  (sendgrid/send-email! (invite->email invite)))
