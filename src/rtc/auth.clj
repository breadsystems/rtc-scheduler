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
    (and (authenticated? req) (two-factor/verified? req))
    :logged-in

    (and (authenticated? req)
         (get form-params "token"))
    :verifying

    (authenticated? req)
    :two-factor

    (and (get form-params "email")
         (get form-params "password"))
    :authenticating

    :else
    :unauthenticated))

(defn login-uri [{:keys [uri query-params] :as req}]
  (let [logged-in? (= :logged-in (login-step req))]
    (if logged-in?
      (get query-params "next" "/admin/volunteer")
      (let [dest (get query-params "next" uri)]
        (format "/login?next=%s" dest)))))

(defn authenticate-request [{:keys [form-params] :as req}]
  (let [user (authenticate-user (get form-params "email")
                                (get form-params "password"))]
    (println user)
    (if user
      (-> (redirect (login-uri req))
          (assoc :session {:identity user}))
      (layout/login-page req))))

(defn verify-two-factor-request [{:keys [form-params] :as req}]
  (if (two-factor/verify-2fa-token (get form-params "token"))
    (-> (redirect (login-uri req))
        (assoc-in [:session :verified-2fa-token?] true))
    (layout/two-factor-page req)))

(defn unauthorized-handler [req _metadata]
  (redirect (login-uri req)))


(defn logout-handler [req]
  (-> (redirect "/login")
      (assoc :session {})))

(defn login-handler [{:keys [query-params form-params session] :as req}]
  (let [email (get form-params "email")
        password (get form-params "password")
        user (authenticate-user email password)]
    (if user
      (-> (redirect (get query-params "next" "/admin/provider"))
          (assoc :session {:identity user}))
      (layout/login-page req))))

(defn wrap-require-auth [handler]
  (fn [req]
    (if (authenticated? req)
      (handler req)
      (throw-unauthorized))))

(def auth-backend
  (session-backend {:unauthorized-handler unauthorized-handler}))


(defn wrap-auth [handler]
  (-> handler
      (wrap-require-auth)
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)))