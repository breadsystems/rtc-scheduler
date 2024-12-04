(ns rtc.auth)

(defn wrap-require-auth [f]
  (fn [{:as req
        session :session
        {:app/keys [authentication env]} :system}]
    (let [session (cond
                    (:user session) session
                    (and (= :dev env) (:dev/user authentication))
                    {:user (:dev/user authentication)}
                    :else session)
          req (assoc req :session session)]
      (if (:user session)
        (f req)
        {:status 302
         :headers
         {:location "/login"}}))))

(defn show-login [_]
  {:body "LOGIN"
   :status 200})
