(ns rtc.auth)

(defn wrap-require-auth [f]
  (fn [req]
    (if (get-in req [:session :user])
      (f req)
      {:status 302
       :headers
       {:location "/login"}})))

(defn show-login [_]
  {:body "LOGIN"
   :status 200})
