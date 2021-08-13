(ns rtc.rest.core
  (:require
   [clojure.data.json :as json]
   [cognitect.transit :as transit]
   [rtc.admin.schedule :as schedule]
   [rtc.auth.core :as auth]
   [rtc.appointments.core :as appt]
   [rtc.users.core :as u])
  (:import
   [clojure.lang ExceptionInfo]
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

(comment
  (->json {:user/id 123}))

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
    {:get (rest-handler (fn [{params :params user :identity}]
                          {:success true
                           :data (appt/get-available-windows params user)}))}]
   ["/appointment"
    {:post (rest-handler (fn [{user :identity :as req}]
                           (try
                             (let [params (transit-params req)]
                               {:success true
                                :data {:appointment
                                       (appt/book-appointment! params user)}})
                             (catch clojure.lang.ExceptionInfo e
                               {:success false
                                :errors [{:message (.getMessage e)
                                          :reason (:reason (ex-data e))}]
                                :data (ex-data e)}))))}]
   ["/register"
    {:post (rest-handler (fn [req]
                           (try
                             (let [user (transit-params req)]
                               (if (u/validate-invitation user)
                                 (do
                                   (u/register! (assoc user
                                                       :is_admin true
                                                       :preferences {}))
                                   {:success true
                                    :data {:redirect-to "/comrades"}})
                                 {:success false
                                  :errors [{:message "Invalid or expired invite code."
                                            :reason :invalid-invite}]}))
                             (catch Throwable e
                               {:success false
                                :errors [:message (.getMessage e)
                                         :reason :unknown]}))))}]
   ["/reset-password"
    {:post (rest-handler (fn [req]
                           (try
                             (let [user (transit-params req)]
                               (if (u/validate-invitation user)
                                 (do
                                   (u/reset-pass! user)
                                   {:success true
                                    :data {:redirect-to "/comrades"}})
                                 {:success false
                                  :errors [{:message "Invalid or expired invite code."
                                            :reason :invalid-invite}]}))
                             (catch Throwable e
                               {:success false
                                :errors [:message (.getMessage e)
                                         :reason :unknown]}))))}]
   ["/admin"
    {:middleware [auth/wrap-auth]}
    ["/schedule"
     {:get (rest-handler (fn [req]
                           (try
                             {:success true
                              :data    (schedule/schedule req)}
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
                              :data (appt/create-note! (transit-params req))}))}]
     ["/need/fulfill"
      {:post (rest-handler (fn [req]
                             {:success true
                              :data (appt/fulfill-need! (transit-params req))}))}]]
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
                                     :data (ex-data e)})))))}]
    ["/invites"
     {:get (rest-handler (fn [{{id :id} :identity :as req}]
                           {:success true
                            :data {:invitations
                                   (map (fn [invite]
                                          (assoc invite
                                                 :url (u/invite-url req invite)
                                                 :expired? (u/expired? invite)))
                                        (u/get-invitations {:invited_by id}))}}))}]

    ["/invite"
     {:post (rest-handler (fn [{{id :id} :identity :as req}]
                            (let [{:keys [email]} (transit-params req)
                                  invitation (u/invite!
                                              {:email email
                                               :invited_by id})
                                  url (u/invite-url req invitation)]
                              {:success true
                               :data (assoc invitation :url url)})))}]
    ["/settings"
     {:get (rest-handler (fn [{{id :id} :identity :as req}]
                           {:success true
                            :data    (u/publicize (u/id->user id))}))
      :post (rest-handler (fn [{{id :id} :identity :as req}]
                            (let [;; Make sure id does not come from user input.
                                  user (assoc (transit-params req) :id id)]
                              (try
                                (u/update-settings! user)
                                {:success true
                                 :data user}
                                (catch ExceptionInfo e
                                  {:success false
                                   :errors [{:message (.getMessage e)
                                             :reason (:reason (ex-data e))}]})))))}]]])

(comment

  ;;
  )
