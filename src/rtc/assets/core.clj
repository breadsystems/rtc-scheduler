(ns rtc.assets.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))


(defn wrap-asset-headers
  "Wrap static resource handlers to cache them for one year."
  [handler]
  (fn [req]
    (let [res (handler req)]
      (assoc-in res [:headers "Cache-Control"] "max-age=31536000"))))

