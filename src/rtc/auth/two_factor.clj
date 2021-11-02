(ns rtc.auth.two-factor
  (:require
   [clj-http.client :as http]
   [clojure.data.json :as json]
   [clojure.walk :as walk]
   [config.core :as config :refer [env]]
   [mount.core :as mount :refer [defstate]]))


(defstate authy-api-key
  :start (let [api-key (:authy-api-key env)]
           (when (empty? api-key)
             (println "WARNING: No Authy API Key detected!"))
           api-key))


(defn- api-call
  ([method endpoint opts]
   (-> (try
         (method (str "https://api.authy.com/protected/json" endpoint)
                 (conj {:headers {"X-Authy-API-Key" authy-api-key}} opts))
         (catch Exception e (ex-data e)))
       :body
       (json/read-str)
       (walk/keywordize-keys)))
  ([method endpoint]
   (api-call method endpoint {})))

(defn user-payload [{:keys [email phone]}]
  {:email email
   :cellphone phone
   :country_code "1"})

(defn app-details []
  (api-call http/get "/app/details"))

(defn create-authy-user! [data]
  (api-call http/post "/users/new" {:form-params {:user (user-payload data)}
                                    :flatten-nested-form-params true}))

(defn get-token [id]
  (api-call http/get (str "/sms/" id)))

(defn verify-token [token id]
  (let [resp (api-call http/get (format "/verify/%s/%s" token id))]
    (->> resp
         (:success)
         (= "true"))))

(defn verified? [req]
  (boolean (:verified-2fa-token? (:session req))))


(comment

  (app-details)

  (def user (create-authy-user! {:email "coby02@cobytamayo.com"
                                 :cellphone "253-222-9139"
                                 :country_code "1"}))

  (verify-token "15 644 34" (get-in user [:user :id]))

  ;;
  )
