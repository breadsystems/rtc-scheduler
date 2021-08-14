(ns rtc.middleware)

(defn read-token [{:keys [params headers]}]
  (or (:__anti-forgery-token params)
      (get headers "x-csrf-token")))
