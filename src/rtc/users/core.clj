(ns rtc.users.core
  (:require
   [buddy.hashers :as hash]
   [crypto.random :as crypto]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.auth.util :as auth-util]
   [rtc.auth.two-factor :as two-factor]
   [rtc.db :as db]
   [rtc.util :refer [one-hour]])
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
                     :email
                     :first_name
                     :last_name
                     :pronouns
                     :phone
                     :state
                     :is_admin
                     :is_provider]))

(defn invite! [{:keys [email invited_by]}]
  (let [invite-code (crypto/url-part 32)
        invitation {:email email
                    :code invite-code
                    :invited_by invited_by
                    :redeemed false
                    :date_invited (Date.)}]
    (db/create-invitation! invitation)
    ;; TODO send email
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

(defn invite-url [{:keys [scheme server-name server-port]} {:keys [email code]}]
  (format "%s://%s/register?email=%s&code=%s"
          (name scheme)
          (if server-port (str server-name ":" server-port) server-name)
          email
          code))

(defn id->user [id]
  (dissoc (db/get-user {:id id}) :pass))

(defn email->user [email]
  (dissoc (db/get-user-by-email {:email email}) :pass))

(defn register! [{:keys [email phone] :as user}]
  (let [authy-payload {:email email
                       :cellphone phone
                       :country_code "1"}
        {authy-user :user} (two-factor/create-authy-user! authy-payload)]
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

(defn update-contact-info! [user]
  (let [user-keys [:first_name :last_name :email :phone :is_provider]]
    (-> (sqlh/update :users)
        (sqlh/sset (select-keys user user-keys))
        (sqlh/where [:= :id (:id user)])
        (sql/format)
        (db/execute!))))

(defn update-settings! [user]
  (if (updating-password? user)
    (update-password! user)
    (update-contact-info! user)))

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

  (email->user "rtc@example.com")
  (admin? (email->user "rtc@example.com"))
  (preferences (email->user "rtc@example.com")))
