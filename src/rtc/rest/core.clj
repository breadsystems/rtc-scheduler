(ns rtc.rest.core
  (:require
   [rtc.auth.core :as auth]
   [rtc.db :as db]
   [cognitect.transit :as transit])
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream]))


(def ^:private default-uid
  (when (= "1" (System/getenv "DEV_DISABLE_AUTH")) 1))

(defn- req->uid [req]
  (get-in req [:session :identity :id] default-uid))

(defn- ->transit [body]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))


(defn endpoints [{:keys [mount]}]
  [mount
   ["/appointment"
    {:post (fn [req]
             {:status 200
              :headers {"Content-Type" "application/transit+edn"}
              :body (->transit {:success true
                                :appointment (:params req)})})}]])

(comment

  (transit/write writer {:hi :there})

  ;;  
  )