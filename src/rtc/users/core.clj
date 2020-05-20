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

(defn register! [user]
  ;; TODO wrap this in a tx
  (let [created (db/create-user! user)]
    (db/redeem-invitation! user)
    created))

(defn id->user [id]
  (dissoc (db/get-user {:id id}) :pass))

(defn email->user [email]
  (dissoc (db/get-user-by-email {:email email}) :pass))

(defn filters->users [filters]
  [])

(comment
  (def invitation (invite! (str (crypto/url-part 6) "@example.com") 1))
  (validate-invitation invitation)
  (validate-invitation (assoc invitation :email "bogus@example.email"))

  (db/get-invitations {:redeemed false :invited_by 1})
  (db/get-invitations {:redeemed true :invited_by 1})

  (register! (conj (select-keys invitation [:code :email])
                   {:pass (crypto/url-part 16)
                    :is_admin false}))

  (email->user "rtc@example.com")
  (admin? (email->user "rtc@example.com"))
  (preferences (email->user "rtc@example.com")))
