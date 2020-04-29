(ns rtc.auth
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [rtc.layout :refer [page]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :refer [redirect]]))


(defn unauthorized-handler [req _metadata]
  (cond
    (authenticated? req)
    {:status 403
     :headers {"Content-Type" "text/plain"}
     :body "Forbidden"}
    :else
    (redirect (format "/login?next=%s" (:uri req)))))

(defn login-handler [{:keys [query-params form-params] :as req}]
  (prn query-params)
  (-> (redirect (:next query-params))
      (assoc :session {:identity {:id 123}})))


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
  (-> handler
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      wrap-session))