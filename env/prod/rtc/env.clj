(ns rtc.env
  (:require
   [ring.middleware.params :refer [wrap-params]]))


(defn middleware [handler]
  (-> handler
      wrap-params))