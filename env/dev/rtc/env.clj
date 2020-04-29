(ns rtc.env
  (:require
   [mount.core :refer [defstate]]
   [nrepl.server :as nrepl]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.reload :refer [wrap-reload]]
   [rtc.auth :refer [wrap-ip-safelist]]))


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
      wrap-params
      (wrap-ip-safelist {:safelist #{"127.0.0.1"}})
      wrap-reload))


(defstate repl-server
  :start (start!)
  :stop  (stop!))