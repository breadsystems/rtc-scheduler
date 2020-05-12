(ns rtc.users.core
  (:require
   [crypto.random :as crypto]
   [rtc.auth.util :as util]
   [rtc.db :as db]))


(defn admin? [user]
  (boolean (:is_admin user)))

(defn provider? [user]
  (boolean (:is_provider user)))

(defn preferences [user]
  (:preferences user))

(defn two-factor-enabled? [user]
  (boolean (:two-factor-enabled? (preferences user))))

(defn invite! [email]
  (let [invite-code (crypto/url-part 32)
        invitation {:email email :code invite-code}]
    (db/create-invitation! invitation)
    ;; TODO send email
    invitation))


(comment
  (invite! "test@example.com"))
