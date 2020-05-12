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

(defn invite! [email inviter-id]
  (let [invite-code (crypto/url-part 32)
        invitation {:email email :code invite-code :invited_by inviter-id}]
    (db/create-invitation! invitation)
    ;; TODO send email
    invitation))

(defn validate-invitation [invitation]
  (boolean (db/get-invitation invitation)))


(comment
  (invite! "test@example.com" 1))
