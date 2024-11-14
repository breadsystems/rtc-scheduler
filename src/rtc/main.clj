(ns rtc.main
  (:require
    [aero.core :as aero]
    [integrant.core :as ig]
    [org.httpkit.server :as http]
    [reitit.core :as reitit]
    [ring.middleware.defaults :as ring]

    [rtc.middleware :refer [wrap-keyword-headers]])
  (:import
    [java.time LocalDateTime]))

(defonce system (atom nil))

(defn app [_]
  {:body "hello, world!"
   :status 200
   :headers
   {:content-type "text/html"}})

;; CONFIG

(defmethod ig/init-key :clojure-version [_ _]
  (clojure-version))

(defmethod ig/init-key :started-at [_ _]
  (LocalDateTime/now))

(defmethod ig/init-key :initial-config [_ config]
  config)

(defmethod ig/init-key :http [_ {:keys [port ring-defaults]
                                 :or {ring-defaults {}}}]
  ;; TODO timbre
  (println "Starting HTTP server on port" port)
  (let [wrap-config (as-> ring/secure-site-defaults $
                        (reduce #(assoc-in %1 (key %2) (val %2)) $ ring-defaults)
                        (assoc-in $ [:params :keywordize] true))
        handler (-> #'app
                    wrap-keyword-headers
                    (ring/wrap-defaults wrap-config))]
    (http/run-server handler {:port port})))

(defmethod ig/halt-key! :http [_ stop-server]
  (when-let [prom (stop-server :timeout 100)]
    @prom))

;; RUNTIME

(defn start! [config]
  (let [config (assoc config
                      :initial-config config
                      :started-at nil
                      :clojure-version nil)]
    (reset! system (ig/init config))))

(defn stop! [_]
  (when-let [sys @system]
    (ig/halt! sys)
    (reset! system nil)))

(defn restart! [config]
  (stop! config)
  (start! config))

(comment

  (restart! (-> "resources/dev.edn" aero/read-config))

  ;;

  )

(defn -main [& file]
  (-> file aero/read-config start!))
