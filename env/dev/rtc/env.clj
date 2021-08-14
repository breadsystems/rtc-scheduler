(ns rtc.env
  (:require
   [config.core :as config]
   [mount.core :as mount :refer [defstate]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.reload :refer [wrap-reload]]
   [rtc.style.build :as style]))


;; Environment variables, all in one place.
;; https://github.com/yogthos/config
(defstate env
  :start (config/load-env))

(comment
 (do
   (mount/stop #'env)
   (mount/start #'env)))

(defn- wrap-prn [handler]
  (fn [req]
    (when (:dev-prn-requests env)
      (prn 'REQUEST req))
    (let [res (handler req)]
      (when (:dev-prn-responses env)
        (prn 'RESPONSE res))
      res)))

(defn- env-anti-forgery
  "Wrap handler in anti-forgery middleware unless explicitly disabled."
  [handler opts]
  (if (:dev-disable-anti-forgery env)
    (do
      (println "NOTICE: Anti-forgery protection is disabled!")
      handler)
    (wrap-anti-forgery handler {:read-token
                                (:anti-forgery/read-token opts)})))

(defn middleware [app opts]
  (-> app
      (env-anti-forgery opts)
      (wrap-prn)
      (wrap-reload)))


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
