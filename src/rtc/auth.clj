(ns rtc.auth
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.hashers :as hash]
   [rtc.auth.two-factor :as two-factor]
   [rtc.auth.util]
   [rtc.db :as db]
   [rtc.layout :as layout]
   [ring.util.response :refer [redirect]]))


(defn authenticate-user [email password]
  (if (and email password)
    (let [user (db/get-user-by-email {:email email})]
      (when (hash/check password (:pass user))
        (dissoc user :pass)))
    nil))

(comment
  (db/get-user-by-email {:email "rtc-admin@example.com"})
  (authenticate-user "rtc-admin@example.com" "bgf7ekabllojGyvZ")
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
    (if (seq dest) dest "/admin/volunteer")))

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

    :logged-in    (redirect
     (destination-uri req))))

(defn wrap-require-auth [handler]
  (fn [req]
    (if (and
        ;;  (authenticated? req)
         (boolean (:identity (:session req)))
         (two-factor/verified? req))
      (handler req)
      (throw-unauthorized))))

(def auth-backend
  (session-backend
   {:unauthorized-handler (fn [{:keys [uri]}]
                            (redirect (format "/login?next=%s" uri)))}))


(defn wrap-auth [handler]
  (-> handler
      (wrap-require-auth)
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)))