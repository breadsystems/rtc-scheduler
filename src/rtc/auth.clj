(ns rtc.auth
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.hashers :as hash]
   [clojure.string :as string]
   [mount.core :as mount :refer [defstate]]
   [rtc.db :as db]
   [rtc.layout :as layout]
   [ring.util.response :refer [redirect]]))


(defn- tmp-password []
  (string/join ""
               (map (fn [_]
                      (rand-nth "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"))
                    (range 0 16))))

(defn- create-first-admin-user! []
  (try
    (let [pw (or (System/getenv "ADMIN_PASSWORD") (tmp-password))
          pw-hash (hash/derive pw)
          email (or (System/getenv "ADMIN_EMAIL") "rtc-admin@example.com")]
      (db/create-user! {:email email :pass pw-hash})
      (println "admin email:" email)
      (println "admin password: " pw))
    (catch Exception e
      (.getMessage e))))

(defstate admin-user
  :start (create-first-admin-user!))


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