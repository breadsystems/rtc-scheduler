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

(defn- ->transit [body]
  (let [out (ByteArrayOutputStream.)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))


(defn- ->json-key [k]
  (if (keyword? k)
    (let [kns (namespace k)]
      (str (if kns (str kns "_") "") (name k)))
    (str k)))

(defn- ->json-value [_ v]
  (let [fmt (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")]
    (cond
      (= Date (type v)) (.format fmt v)
      (= Timestamp (type v)) (.format fmt v)
      :else v)))

(defn- ->json [x]
  (json/write-str x :key-fn ->json-key :value-fn ->json-value))

(->json {:user/id 123})

(defn- rest-handler [f]
  (fn [{:keys [params] :as req}]
    (let [;; The frontend always consumes application/transit+edn data,
          ;; but application/json is useful for debugging
          json? (boolean (:json params))
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
   ["/admin"
    {:middleware [auth/wrap-auth]}
    ["/schedule"
     {:get (rest-handler (fn [req]
                           (try
                             {:success true
                              :data    (merge {:user {:id 1}}
                                              (schedule/schedule req))}
                             (catch Throwable e
                               {:success false
                                :errors [{:message (.getMessage e)}]}))))}]
    ["/appointment"
     [""
      {:get (rest-handler (fn [{:keys [params]}]
                            {:success true
                             :data    (appt/details (:id params))}))}]
     ["/note"
      {:post (rest-handler (fn [req]
                             {:success true
                              :data (appt/create-note! (transit-params req))}))}]]
    ["/availability"
     {:post (rest-handler (fn [req]
                            (try
                              {:success true
                               :data    {:availability (appt/schedule-availability! (transit-params req))}}
                              (catch clojure.lang.ExceptionInfo e
                                {:success false
                                 :errors [{:message (.getMessage e)
                                           :reason (:reason (ex-data e))}]
                                 :data (ex-data e)}))))
      :patch (rest-handler (fn [req]
                             (try
                               {:success true
                                :data    {:availability (appt/update-availability! (transit-params req))}}
                               (catch clojure.lang.ExceptionInfo e
                                 {:success false
                                  :errors [{:message (.getMessage e)
                                            :reason (:reason (ex-data e))}]
                                  :data (ex-data e)}))))
      :delete (rest-handler (fn [req]
                              (let [avail (transit-params req)]
                                (try
                                  {:success true
                                   :data {:availability (appt/delete-availability! avail)}}
                                  (catch clojure.lang.ExceptionInfo e
                                    {:success false
                                     :errors [{:message (.getMessage e)
                                               :reason (:reason (ex-data e) :unexpected-error)}]
                                     :data (ex-data e)})))))}]]])

(comment

  ;;
  )