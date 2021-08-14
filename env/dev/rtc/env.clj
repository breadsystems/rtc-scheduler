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

(defn- wrap-dev-identity
  "Load default dev admin user into the session identity when auth is
  explicitly disabled. Note that the wrap-identity middleware loads the actual
  :identity key into req."
  [handler opts]
  (if (:dev-disable-auth env)
    (fn [req]
      (handler (assoc-in req [:session :identity] (:auth/default-user opts))))
    handler))

(defn middleware [app opts]
  (-> app
      (wrap-reload)
      (wrap-prn)
      (wrap-dev-identity opts)
      (env-anti-forgery opts)))


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
