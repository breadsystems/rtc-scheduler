(ns rtc.rest.core
  (:require
   [clojure.data.json :as json]
   [cognitect.transit :as transit]
   [rtc.admin.schedule :as schedule]
   [rtc.auth.core :as auth]
   [rtc.appointments.core :as appt])
  (:import
    [java.io ByteArrayOutputStream]
    [java.lang Throwable]
    [java.sql Timestamp]
    [java.util Date]
    [java.text SimpleDateFormat]))


(def ^:private default-uid
  (when (= "1" (System/getenv "DEV_DISABLE_AUTH")) 1))

(defn- req->uid [req]
  (get-in req [:session :identity :id] default-uid))

(defn- ->transit [body]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))


(defn- ->json-value [_ v]
  (let [fmt (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")]
    (cond
      (= Date (type v)) (.format fmt v)
      (= Timestamp (type v)) (.format fmt v)
      :else v)))

(defn- ->json [x]
  (json/write-str x :value-fn ->json-value))

(defn- rest-handler [f]
  (fn [req]
    (let [;; The frontend always consumes application/transit+edn data,
          ;; but application/json is useful for debugging
          json? (boolean (get (:params req) "json"))
          transform (if json? ->json ->transit)
          content-type (if json? "application/json" "application/transit+edn")
          res (f req)
          status (if (:success res) 200 400)]
      {:status  status
       :headers {"Content-Type" content-type}
       :body    (transform {:success (:success res)
                            :data    (:data res)
                            :errors  (:errors res)})})))

(defn- transit-params [{:keys [body]}]
  (let [reader (transit/reader body :json)]
    (transit/read reader)))

(defn endpoints [{:keys [mount]}]
  [mount
   ["/windows"
    {:get (rest-handler (fn [{:keys [params]}]
                          {:success true
                           :data (appt/get-available-windows params)}))}]
   ["/appointment"
    {:post (rest-handler (fn [req]
                           (try
                             {:success true
                              :data {:appointment (appt/book-appointment! (transit-params req))}}
                             (catch clojure.lang.ExceptionInfo e
                               {:success false
                                :errors [{:message (.getMessage e)
                                          :reason (:reason (ex-data e))}]
                                :data (ex-data e)}))))}]
   ["/schedule"
    ;; TODO AUTHORIZE REQUEST!!
    {:get (rest-handler (fn [req]
                          (try
                            {:success true
                             :data    (merge {:user {:id 1}}
                                             (schedule/schedule req))}
                            (catch Throwable e
                              {:success false
                               :errors [{:message "Unexpected error"}]}))))}]])

(comment

  ;;
  )