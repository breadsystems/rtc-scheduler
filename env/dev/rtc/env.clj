(ns rtc.env
  (:require
   [config.core :as config]
   [mount.core :as mount :refer [defstate]]
   [ring.middleware.reload :refer [wrap-reload]]
   [rtc.style.build :as style]))


(defn middleware [app]
  (-> app
      (wrap-reload)))


(def ^:private style-namespaces #{'rtc.intake.style
                                  'foo
                                  'rtc.style.core})

;; TODO use Thread.interrupt() or similar to :stop watch
(defstate garden-watcher
  :start (do
           (println "Watching Garden for changes...")
           (style/watch! {:source-paths ["src/rtc/intake" "src/rtc/style"]
                          :styles 'rtc.intake.style/screen
                          :compiler {:output-to "resources/public/css/intake.css"}})
           (style/watch! {:source-paths ["src/rtc/admin" "src/rtc/style"]
                          :styles 'rtc.admin.style/screen
                          :compiler {:output-to "resources/public/css/admin.css"}})))


(comment
 (do
   (mount/stop #'garden-watcher)
   (mount/start #'garden-watcher)))