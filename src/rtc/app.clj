(ns rtc.app
  (:require
   [org.httpkit.server :as http]
   [mount.core :as mount :refer [defstate]]
   [reitit.ring :as ring]
   [rtc.env]))


(defn handler [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello, Comrades!"})

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get handler}]
     ["/ping" (constantly {:status 200
                           :headers {"Content-Type" "text/plain; charset=utf-8"}
                           :body "OK"})]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler))))


(defonce stop-http (atom nil))

(defn start! []
  (let [port (Integer. (or (System/getenv "HTTP_PORT") 8080))]
    (println (str "Running HTTP server at localhost:" port))
    (reset! stop-http (http/run-server app {:port port})))
  nil)

(defn stop! []
  (println "Stopping HTTP server")
  (when (fn? @stop-http)
    (@stop-http))
  (reset! stop-http nil))

(defstate http-server
  :start (start!)
  :stop  (stop!))


(defn -main [& args]
  (mount/start))