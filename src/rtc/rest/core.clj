(ns rtc.rest.core
  (:require
   [clojure.data.json :as json]
   [cognitect.transit :as transit]
   [rtc.auth.core :as auth]
   [rtc.appointments.core :as appt])
  (:import
   [java.io ByteArrayOutputStream]))


(def ^:private default-uid
  (when (= "1" (System/getenv "DEV_DISABLE_AUTH")) 1))

(defn- req->uid [req]
  (get-in req [:session :identity :id] default-uid))

(defn- ->transit [body]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))

(defn- rest-handler [f]
  (fn [req]
    (let [;; The frontend always consumes application/transit+edn data,
          ;; but application/json is useful for debugging
          json? (boolean (get (:params req) "json"))
          transform (if json? json/write-str ->transit)
          content-type (if json? "application/json" "application/transit+edn")
          res (f req)
          success? (:success res)]
      (if success?
        {:status  200
         :headers {"Content-Type" content-type}
         :body    (transform {:success true
                              :data    (:data res)})}
        {:status 400
         :headers {"Content-Type" content-type}
         :body    (transform {:success false
                              :errors  (:errors req)})}))))


(defn endpoints [{:keys [mount]}]
  [mount
   ["/windows"
    {:get (rest-handler (fn [req]
                          {:success true
                           :data    (:params req)}))}]
   ["/appointment"
    {:post (fn [req]
             {:status 200
              :headers {"Content-Type" "application/transit+edn"}
              :body (->transit {:success true
                                :appointment (:params req)})})}]])

(comment

  ;;
  )