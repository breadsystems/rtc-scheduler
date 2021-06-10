(ns rtc.auth.core
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [mount.core :refer [defstate]]
   [ring.util.response :refer [redirect]]
   [rtc.auth.two-factor :as two-factor]
   [rtc.auth.util :as util]
   [rtc.env :refer [env]]
   [rtc.layout :as layout]
   [rtc.users.core :as u]))


;; Create an admin user on first startup, if one does not exist
(defstate admin-user
  :start (util/create-first-admin-user!))

(defstate default-user
  :start (let [user (u/email->user (:default-admin-email env))]
             (-> (select-keys user [:id :authy_id])
                 (assoc :admin? (:is_admin user)
                        :provider? (boolean (:is_provider user))))))


(defn- auth-disabled? []
  (boolean (:dev-disable-auth env)))

(comment
  (auth-disabled?)
  (u/email->user "rtc@example.com")
  (u/admin? (u/email->user "rtc@example.com"))
  (u/preferences (u/email->user "rtc@example.com"))
  (u/authenticate "rtc-admin@example.com" "[PASSWORD HERE]")
  (u/authenticate "rtc-admin@example.com" "garbage"))


(defn- logged-in? [req]
  (and (authenticated? req) (two-factor/verified? req)))

(defn login-step [{:keys [params] :as req}]
  (cond
    (logged-in? req)
    :logged-in

    (and (authenticated? req) (:token params))
    :verifying

    (authenticated? req)
    :two-factor

    (and (:email params) (:password params))
    :authenticating

    :else
    :unauthenticated))

(defn destination-uri [{:keys [params]}]
  (if (seq (:next params))
    (:next params)
    "/comrades"))

(defn logout-handler [_req]
  (-> (redirect "/login")
      (assoc :session {})))

(defn on-token-failure [{:keys [session] :as req}]
  (let [token-attempts (or (:token-attempts session) 1)]
    (if (>= token-attempts 3)
      (-> (redirect "/login")
          ;; Too many attempts. Wipe out login state.
          (assoc :session {}))
      (-> (layout/two-factor-page {:req req :error "Invalid token."})
          ;; Record token failure count.
          (assoc :session
                 (assoc session :token-attempts (inc token-attempts)))))))

(defn login-handler [{:keys [params session identity] :as req}]
  (condp = (login-step req)
    :unauthenticated
    (layout/login-page {:req req})

    :authenticating
    (if-let [user (u/authenticate (:email params) (:password params))]
      (let [;; Persist our user identity in a new session.
            session (-> (:session req)
                        (assoc :identity (select-keys user [:id :authy_id]))
                        (vary-meta assoc :recreate true))]
        (u/record-login! user)
        (-> (layout/two-factor-page {:req req})
          (assoc :session session)))
      (layout/login-page {:req req
                          :error "Invalid email or password"}))

    :two-factor
    (layout/two-factor-page {:req req})

    :verifying
    (if (two-factor/verify-token (:token params) (:authy_id identity))
      (-> (redirect (destination-uri req))
          ;; assoc-in doesn't work here because magic
          (assoc :session (assoc session :verified-2fa-token? true)))
      (on-token-failure req))

    :logged-in
    (redirect (destination-uri req))))

(defn wrap-identity
  "Persist session identity directly into request"
  [handler]
  (fn [req]
    (handler (assoc req :identity (get-in req [:session :identity])))))

(defn wrap-require-auth [handler]
  (fn [req]
    ;; Support disabling auth for nicer frontend dev workflow
    (if (or (logged-in? req) (auth-disabled?))
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
