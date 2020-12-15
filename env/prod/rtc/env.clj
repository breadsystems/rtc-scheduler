(ns rtc.env
  (:require
   [config.core :as config]
   [garden.core :as garden]
   [mount.core :as mount :refer [defstate]]
   [rtc.admin.style :as admin-style]
   [rtc.style.build :as build]
   [rtc.intake.style :as intake-style]))


;; Environment variables, all in one place.
;; https://github.com/yogthos/config
(defstate env
  :start (config/load-env))


(defn middleware [handler]
  handler)

(defn- build-styles []
  (println "Compiling styles...")
  (build/compile-styles! [{:name :intake
                           :styles intake-style/screen
                           :opts {:pretty-print? false}}
                          {:name :admin
                           :styles admin-style/screen
                           :opts {:pretty-print? false}}]))
  
(comment
  (apply garden/css {:pretty-print? false} admin-style/screen))


(defn -main []
  (build-styles))