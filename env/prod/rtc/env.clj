(ns rtc.env
  (:require
   [garden.core :as garden]
   [rtc.admin.style :as admin-style]
   [rtc.intake.style :as intake-style]))


(defn middleware [handler]
  handler)

(defn- build-styles []
  (println "Compiling styles...")
  (apply garden/css
         {:pretty-print? false
          :output-to "resources/public/css/intake.css"}
         intake-style/screen)
  (apply garden/css
         {:pretty-print? false
          :output-to "resources/public/css/admin.css"}
         admin-style/screen))


(defn -main []
  (build-styles))