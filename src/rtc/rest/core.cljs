(ns rtc.rest.core
  (:require
    [cljs-http.client :as http]))


(http/get "/windows" {:form-params {:state "WA"}})

(js/console.log 'hi)