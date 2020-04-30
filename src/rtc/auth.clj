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
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :refer [redirect]]))


(defn- rand-password []
  (string/join ""
               (map (fn [_]
                      (rand-nth "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"))
                    (range 0 16))))

(defn- create-first-admin-user! []
  (try
    (let [pw (or (System/getenv "ADMIN_PASSWORD") (rand-password))
          pw-hash (hash/derive pw)
          email (or (System/getenv "ADMIN_EMAIL") "rtc-admin@example.com")]
      (db/create-user! {:email email :pass pw-hash})
      (println "admin email:" email)
      (println "admin password: " pw))
    (catch Exception e
      (.getMessage e))))

(defstate admin-user
  :start (create-first-admin-user!))


(defn unauthorized-handler [req _metadata]
  (cond
    (authenticated? req)
    {:status 403
     :headers {"Content-Type" "text/plain"}
     :body "Forbidden"}
    :else
    (redirect (format "/login?next=%s" (:uri req)))))

(defn authenticate-user [email password]
  (if (and email password)
    (let [user (db/get-user-by-email {:email email})]
      (when (hash/check password (:pass user))
        user))
    nil))

(comment
  (db/get-user-by-email {:email "rtc-admin@example.com"})
  (authenticate-user "rtc-admin@example.com" "bgf7ekabllojGyvZ")
  (authenticate-user "rtc-admin@example.com" "garbage"))

(defn login-handler [{:keys [query-params form-params session] :as req}]
  (let [email (get form-params "email")
        password (get form-params "password")
        user (authenticate-user email password)]
    (if user
      (-> (redirect (get query-params "next" "/dashboard"))
          (assoc-in [:session :identity] user))
      (layout/login-page req))))


;; Define middlewares

(defn- request->ip [req]
  (or (get-in req [:headers "x-forwarded-for"])
      (:remote-addr req)
      ""))

(defn wrap-ip-safelist [handler {:keys [safelist]}]
  (fn [req]
    (if (contains? safelist (request->ip req))
      (handler req)
      {:status 401
       :headers {"Content-Type" "text/plain"}
       :body "Not allowed"})))

;; TODO redirect to an actual login page
(def auth-backend
  (session-backend {:unauthorized-handler unauthorized-handler}))

(defn wrap-auth [handler]
  (-> (fn [req]
        (if (authenticated? req)
          (handler req)
          (throw-unauthorized)))
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      wrap-session))