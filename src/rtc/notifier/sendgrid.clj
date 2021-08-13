(ns rtc.notifier.sendgrid
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.string :as string]
    [config.core :as config :refer [env]]
    [mount.core :as mount :refer [defstate]]))

(defstate sendgrid-api-key
  :start (let [api-key (:sendgrid-api-key env)]
           (when (empty? api-key)
             (println "WARNING: No SendGrid API Key detected!"))
           api-key))

(defstate sendgrid-from-email
  :start (let [from (:sendgrid-from-email env)]
           (when (empty? from)
             (printf "WARNING: No SendGrid FROM address detected!"))
           from))

;; curl --request POST \
;;   --url https://api.sendgrid.com/v3/mail/send \
;;   --header "Authorization: Bearer $SENDGRID_API_KEY" \
;;   --header 'Content-Type: application/json' \
;;   --data '{"personalizations": [{"to": [{"email": "test@example.com"}]}],
;;            "from": {"email": "test@example.com"},
;;            "subject": "Sending with SendGrid is Fun",
;;            "content": [{"type": "text/plain",
;;                         "value": "and easy to do anywhere, even with cURL"}]}'
(defn- api-call
  ([method endpoint opts]
   (let [uri (str "https://api.sendgrid.com" endpoint)]
     (method uri {:headers {"Authorization" (str "Bearer " sendgrid-api-key)
                            "Content-Type" "application/json"}
                  :content-type :json
                  :form-params (:form-params opts)}))))

(defn- mail-send-form-params [{:keys [to to-name from subject message]}]
  {:personalizations [{:to [{:email to
                             :name to-name}]}]
   :from {:email from
          :name "Radical Telehealth Collective"}
   :subject "Your appointment with the Radical Telehealth Collective"
   :content [{:type "text/plain"
              :value message}]})

(defn send-email! [{:keys [message to to-name]}]
  (when (and message to)
    (api-call http/post "/v3/mail/send"
              {:form-params (mail-send-form-params
                              {:to to
                               :to-name to-name
                               :from sendgrid-from-email
                               :message message})})))

(comment
  (api-call http/post "/v3/mail/send" {})
  (mail-send-form-params {:to "coby@tamayo.email"
                          :to-name "Coby Test"
                          :from "info@radicaltelehealthcollective.org"
                          :message "hello this is a test."})

  ;; These are equivalent:
  (api-call http/post "/v3/mail/send"
            {:form-params
             (mail-send-form-params
               {:to "coby@tamayo.email"
                :to-name "Coby Test"
                :from "info@radicaltelehealthcollective.org"
                :message "hello this is a test."})})
  (send-email! {:to "coby@tamayo.email"
                :to-name "Coby Test"
                :from "info@radicaltelehealthcollective.org"
                :message "hello this is a test."})

  sendgrid-api-key
  sendgrid-from-email
  )
