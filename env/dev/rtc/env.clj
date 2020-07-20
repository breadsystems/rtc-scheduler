(ns rtc.env
  (:require
   [clojure.set :refer [intersection]]
   [mount.core :as mount :refer [defstate]]
   [nrepl.server :as nrepl]
   [ring.middleware.reload :refer [wrap-reload]]
   [rtc.style.build :as style]))


(defonce stop-repl (atom nil))

(defn start! []
  (println (str "Starting nREPL server at localhost:7000"))
  (reset! stop-repl (nrepl/start-server :port 7000))
  (spit ".nrepl-port" "7000")
  nil)

(defn stop! []
  (when @stop-repl
    (@stop-repl)
    (reset! stop-repl nil))
  nil)


(defn middleware [app]
  (-> app
      (wrap-reload)))


(defstate repl-server
  :start (start!)
  :stop  (stop!))


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