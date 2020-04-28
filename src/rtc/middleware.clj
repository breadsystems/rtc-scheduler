(ns rtc.middleware)


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