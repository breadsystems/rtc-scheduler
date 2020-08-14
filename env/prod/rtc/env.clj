(ns rtc.env
  (:require
   [garden.core :as garden]
   [mount.core :refer [defstate]]
   [rtc.admin.style :as admin-style]
   [rtc.intake.style :as intake-style]))


(defn middleware [handler]
  handler)


(defn -main []
  (apply garden/css
         {:pretty-print? false
          :output-to "resources/public/css/intake.css"}
         intake-style/screen)
  (apply garden/css
         {:pretty-print? false
          :output-to "resources/public/css/admin.css"}
         admin-style/screen))