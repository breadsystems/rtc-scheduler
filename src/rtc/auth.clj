(ns rtc.auth
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.hashers :as hash]
   [rtc.auth.two-factor :as two-factor]
   [rtc.users.core :as u]
   [rtc.auth.util]
   [rtc.db :as db]
   [rtc.layout :as layout]
   [ring.util.response :refer [redirect]]))


(defn- auth-disabled? []
  (= "1" (System/getenv "DEV_DISABLE_AUTH")))

(defn authenticate-user [email password]
  (if (and email password)
    (let [user (db/get-user-by-email {:email email})]
      (when (hash/check password (:pass user))
        (dissoc user :pass)))
    nil))

(comment
  (u/email->user "rtc@example.com")
  (u/admin? (u/email->user "rtc@example.com"))
  (u/preferences (u/email->user "rtc@example.com"))
  (u/two-factor-enabled? (u/email->user "rtc@example.com"))
  (authenticate-user "rtc-admin@example.com" "[PASSWORD HERE]")
  (authenticate-user "rtc-admin@example.com" "garbage"))


(defn login-step [{:keys [form-params] :as req}]
  (cond
    (and
     ;; (authenticated? req)
     (boolean (:identity (:session req)))
     (two-factor/verified? req))
    :logged-in

    (and ;; (authenticated? req)
     (boolean (:identity (:session req)))
     (get form-params "token"))
    :verifying

    ;; (authenticated? req)
    (boolean (:identity (:session req)))
    :two-factor

    (and (get form-params "email")
         (get form-params "password"))
    :authenticating

    :else
    :unauthenticated))

(defn destination-uri [{:keys [query-params]}]
  (let [dest (get query-params "next")]
    (if (seq dest) dest "/comrades")))

(defn logout-handler [_req]
  (-> (redirect "/login")
      (assoc :session {})))

(defn login-handler [{:keys [form-params session] :as req}]
  (condp = (login-step req)
    :unauthenticated
    (layout/login-page {:req req})

    :authenticating
    (if-let [user (authenticate-user (get form-params "email")
                                     (get form-params "password"))]
      (assoc (layout/two-factor-page req) :session {:identity user})
      (layout/login-page {:req req
                          :error "Invalid email or password"}))

    :two-factor
    (layout/two-factor-page req)

    :verifying
    (if (two-factor/verify-token (get form-params "token") 25490095)
      (-> (redirect (destination-uri req))
          (assoc :session (assoc session :verified-2fa-token? true)))
      (layout/two-factor-page {:req req
                               :error "Invalid token"}))

    :logged-in
    (redirect (destination-uri req))))

(defn admin-only-resolver [resolver]
  (fn [{:keys [request] :as context} query-string value]
    (if (or (auth-disabled?) (u/admin? (:identity (:session request))))
      (resolver context query-string value)
      {:errors [{:message "You do not have permission to do that"}]})))

(defn wrap-require-auth [handler]
  (fn [req]
    (if (or
         (and
          ;;  (authenticated? req)
          (boolean (:identity (:session req)))
          (two-factor/verified? req))
         ;; Support disabling auth for nicer frontend dev workflow
         (auth-disabled?))
      (handler req)
      (throw-unauthorized))))

(def auth-backend
  (session-backend
   {:unauthorized-handler (fn [{:keys [uri]} _metadata]
                            (redirect (format "/login?next=%s" uri)))}))


(defn wrap-auth [handler]
  (-> handler
      (wrap-require-auth)
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)))