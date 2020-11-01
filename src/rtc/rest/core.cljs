(ns rtc.rest.core
  (:require
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]
   [cognitect.transit :as transit]
   [re-frame.core :as rf])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


(defn request! [http-method endpoint req then]
  (go (let [response (<! (http-method endpoint req))
            reader (transit/reader :json)
            data (transit/read reader (:body response))]
        (cond
          (nil? then) (prn data)
          (fn? then) (then data)
          (keyword? then) (rf/dispatch [then data])))))

(defn get! [& args]
  (apply request! http/get args))

(defn post! [& args]
  (apply request! http/post args))

(comment
  (js/console.clear)
  (get! "/api/v1/windows" {:form-params {:state "WA"}} :foo))