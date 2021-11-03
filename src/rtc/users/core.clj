(ns rtc.users.core
  (:require
   [buddy.hashers :as hash]
   [crypto.random :as crypto]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.auth.util :as auth-util]
   [rtc.auth.two-factor :as two-factor]
   [rtc.db :as db]
   [rtc.util :refer [one-hour url-encode]])
  (:import
   [java.util Date]))


(defn admin? [user]
  (boolean (:is_admin user)))

(defn provider? [user]
  (boolean (:is_provider user)))

(defn preferences [user]
  (:preferences user))

(defn publicize
  "Return the set of public user fields we want to be viewable on the frontend."
  [user]
  (select-keys user [:id
                     :authy_id
                     :email
                     :first_name
                     :last_name
                     :pronouns
                     :phone
                     :state
                     :is_admin
                     :is_provider]))

(defn all []
  (map publicize (db/get-all-users)))

(defn invite! [{:keys [email invited_by]}]
  (let [invite-code (crypto/url-part 32)
        invitation {:email email
                    :code invite-code
                    :invited_by invited_by
                    :redeemed false
                    :date_invited (Date.)}]
    (db/create-invitation! invitation)
    invitation))

(defn validate-invitation [invitation]
  (boolean (db/get-invitation invitation)))

(defn expired? [{:keys [date_invited]}]
  (>= (- (inst-ms (Date.)) (* 72 one-hour)) (inst-ms date_invited)))

(defn get-invitations [{:keys [invited_by]}]
  (-> (sqlh/select :*)
      (sqlh/from :invitations)
      (sqlh/where [:= :invited_by invited_by])
      (sqlh/order-by [[:date_invited :desc]])
      (sql/format)
      (db/query)))

(defn id->user [id]
  (dissoc (db/get-user {:id id}) :pass))

(defn email->user [email]
  (dissoc (db/get-user-by-email {:email email}) :pass))

(defn invite-url
  [{:keys [scheme server-name server-port]} {:keys [email code]}]
  (let [port (str (when (and server-port (not= 80 server-port))
                    (str ":" server-port)))
        register-or-reset-pw (if (email->user email)
                               "reset-password"
                               "register")]
    (format "%s://%s%s/%s?email=%s&code=%s"
            (name scheme)
            server-name
            port
            register-or-reset-pw
            (url-encode email)
            code)))

(defn register! [{:keys [email phone] :as user}]
  (let [{authy-user :user} (two-factor/create-authy-user! user)]
    (-> user
        (update :pass hash/derive)
        (assoc :authy_id (:id authy-user))
        (db/create-user!)))
  (db/redeem-invitation! user)
  (email->user (:email user)))

(defn updating-password? [{:keys [pass]}]
  (> (count pass) 0))

(defn update-password! [{:keys [id pass pass-confirmation]}]
  (when (not= pass pass-confirmation)
    (throw (ex-info "Password and confirmation do not match!"
                    {:reason :pass-confirmation-mismatch})))
  (db/execute!
   ["UPDATE users SET pass = ? WHERE id = ?" (hash/derive pass) id]))

(defn reset-pass! [{:keys [email] :as user}]
  ;; We don't have the user's ID yet, so get them by their email.
  (update-password! (merge (email->user email) user))
  ;; This invitation is no longer valid!
  (db/redeem-invitation! user)
  ;; Get the fresh user data.
  (email->user email))

(defn update-contact-info! [user]
  (let [user-keys [:first_name
                   :last_name
                   :pronouns
                   :email
                   :phone
                   :state
                   :is_provider]]
    (-> (sqlh/update :users)
        (sqlh/sset (select-keys user user-keys))
        (sqlh/where [:= :id (:id user)])
        (sql/format)
        (db/execute!))))

(defn update-authy-id! [{:keys [id authy_id]}]
  (-> (sqlh/update :users)
      (sqlh/sset {:authy_id authy_id})
      (sqlh/where [:= :id id])
      (sql/format)
      (db/execute!)))

(defn update-settings! [user]
  (if (updating-password? user)
    (update-password! user)
    (let [old-user (id->user (:id user))
          updating-phone? (not= (:phone old-user) (:phone user))]
      (when updating-phone?
        (let [authy-user (:user (two-factor/create-authy-user! user))]
          (update-authy-id! {:id (:id user)
                             :authy_id (:id authy-user)})))
      (update-contact-info! user))))

(defn authenticate [email password]
  (when (and email password)
    (when-let [user (db/get-user-by-email {:email email})]
      (when (hash/check password (:pass user))
        ;; We won't ever need the password hash, except in this context,
        ;; so hide it from the caller.
        (dissoc user :pass)))))

(defn record-login! [{:keys [id]}]
  (db/execute! ["UPDATE users SET last_login = NOW() WHERE id = ?" id]))

(comment
  (def shevek (assoc (email->user "shevek@tamayo.email")
                     :phone "1234567890"))

  (update-authy-id! {:id (:id shevek)
                     :authy_id nil})

  (update-settings! shevek)
  (update-settings! (assoc shevek :phone "5005550006"))
  (select-keys (email->user "shevek@tamayo.email") [:id :phone :authy_id])

  ;;

  (def admin (email->user "rtc@tamayo.email"))

  (def invitation (invite! {:email (str (crypto/url-part 6) "@example.com")
                            :invited_by (:id admin)}))
  (validate-invitation invitation)
  (validate-invitation (assoc invitation :email "bogus@example.email"))

  (get-invitations {:invited_by (:id admin)})

  (invite-url {:scheme :http :server-name "localhost" :server-port "8080"} invitation)

  (def user (merge (select-keys invitation [:code :email])
                   {:pass (auth-util/tmp-password)
                    :first_name "New"
                    :last_name "User"
                    :pronouns "they/them"
                    :phone "2532229139"
                    :state "WA"
                    :is_admin true
                    :authy_id nil
                    :preferences {}}))

  user
  (register! user)

  (def reset-invite (invite! {:email "coby@tamayo.email"
                              :invited_by (:id admin)}))
  (validate-invitation reset-invite)

  (invite-url {:scheme :http :server-name "localhost" :server-port "8080"}
              reset-invite)

  (email->user "rtc@example.com")
  (admin? (email->user "rtc@example.com"))
  (preferences (email->user "rtc@example.com")))
