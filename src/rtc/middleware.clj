(ns rtc.middleware)

(defn wrap-keyword-headers [f]
  (fn [req]
    (let [res (f req)]
      (update res :headers clojure.walk/stringify-keys))))
