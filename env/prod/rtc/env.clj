(ns rtc.env
  (:require
   [garden.core :as garden]
   [mount.core :refer [defstate]]
   [rtc.intake.style :as intake-style]))


(defstate css
  ;; TODO MOAR STYLEZ
  :start (apply garden/css
                {:pretty-print? false
                 :output-to "resources/public/css/intake.css"}
                intake-style/screen))


(defn middleware [handler]
  handler)