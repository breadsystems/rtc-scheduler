(ns rtc.notifier.twilio
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.string :as string]
    [clojure.walk :as walk]
    [config.core :as config :refer [env]]
    [mount.core :as mount :refer [defstate]]))

(defstate account-sid
  :start (let [sid (:twilio-account-sid env)]
           (when (empty? sid)
             (println "WARNING: No Twilio Account SID detected!"))
           sid))

(defstate auth-token
  :start (let [token (:twilio-auth-token env)]
           (when (empty? token)
             (println "WARNING: No Twilio Auth Token detected!"))
           token))

(defstate twilio-number
  :start (let [number (:twilio-number env)]
           (when (empty? number)
             (println "WARNING: No Twilio Number detected!"))
           number))

(defn- api-call
  ([method endpoint opts]
   (-> (try
         (method (str "https://api.twilio.com/2010-04-01" endpoint)
                 ;; -u $TWILIO_ACCOUNT_SID:$TWILIO_AUTH_TOKEN
                 (conj {:basic-auth [account-sid auth-token]} opts))
         (catch Exception e (ex-data e)))
       :body
       (json/read-str)
       (walk/keywordize-keys)))
  ([method endpoint]
   (api-call method endpoint {})))

(defn us-phone [phone]
  (let [phone (.replaceAll phone "[^0-9]" "")]
    (cond
      (string/starts-with? phone "1") (str "+" phone)
      :else (str "+1" phone))))

(defn send-sms! [{:keys [message to]}]
  (api-call http/post
            (str "/Accounts/" account-sid "/Messages.json")
            {:form-params {:Body message
                           :To (us-phone to)
                           :From twilio-number}}))

(comment

  (and (= "+12535551234" (us-phone "2535551234"))
       (= "+12535551234" (us-phone "253 555 1234"))
       (= "+12535551234" (us-phone "1 253 555 1234"))
       (= "+12535551234" (us-phone "+1 253 555 1234")))

  ;; Basic test hitting the API directly.
  (http/post
    (format
      "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json"
      account-sid)
    {:form-params {:Body "This is a basic test"
                   :From twilio-number
                   :To "+12532229139"}
     :basic-auth [account-sid auth-token]})

  (send-sms! {:message "This is a test via send-sms!"
              :to "+12532229139"})

  ;;
  )
