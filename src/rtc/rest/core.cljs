(ns rtc.rest.core
  (:require
   [cljs.core.async :refer [<!]]
   [cljs-http.client :as http]
   [cognitect.transit :as transit]
   [re-frame.core :as rf])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


(defn request! [http-method endpoint req then on-err]
  (go (let [response (<! (http-method endpoint req))
            reader (transit/reader :json)]
        (try
          (let [data (transit/read reader (:body response))]
           (cond
             (nil? then) (prn data)
             (fn? then) (then data)
             (keyword? then) (rf/dispatch [then data])))
          (catch :default e
            (cond
              (nil? on-err) (prn e)
              (fn? on-err) (on-err e)
              (keyword? then) (rf/dispatch [on-err e])))))))

(defn get! [& args]
  (apply request! http/get args))

(defn post! [& args]
  (apply request! http/post args))

(defn delete! [& args]
  (apply request! http/delete args))

(comment
  (js/console.clear)
  (get! "/api/v1/windows" {:form-params {:state "WA"}} :foo))