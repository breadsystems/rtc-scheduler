(ns rtc.auth
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.hashers :as hash]
   [rtc.auth.two-factor :as two-factor]
   [rtc.auth.util :as util]
   [rtc.db :as db]
   [rtc.layout :as layout]
   [ring.util.response :refer [redirect]]))


(defn verified-2fa-token? [req]
  (boolean (:verified-2fa-token? (:session req))))

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


(defn unauthorized-handler [req _metadata]
  (cond
    ;; authenticated, but not authorized
    (authenticated? req)
    {:status 403
     :headers {"Content-Type" "text/plain"}
     :body "Forbidden"}
    :else
    (redirect (format "/login?next=%s" (:uri req)))))

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

(defn require-authentication [handler]
  (fn [req]
    (if (authenticated? req)
      (handler req)
      (throw-unauthorized))))

(def auth-backend
  (session-backend {:unauthorized-handler unauthorized-handler}))


(defn wrap-auth [handler]
  (-> handler
      (require-authentication)
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)))