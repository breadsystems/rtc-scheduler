(ns rtc.app
  (:require
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [mount.core :as mount :refer [defstate]]
   [org.httpkit.server :as http]
   [reitit.ring :as ring]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [rtc.api :as api]
   [rtc.auth :as auth]
   [rtc.db]
   [rtc.env :refer [middleware]]))


(def app
  (ring/ring-handler
   (ring/router
    [""
     {:middleware [wrap-anti-forgery]}
     ["/api/graphql" {:post (fn [req]
                              {:status 200
                               :headers {"Content-Type" "application/edn"}
                               :body (-> req :body slurp api/q)})}]
     ["/login" auth/login-handler]
     ["/provider" {:middleware [auth/wrap-auth]
                   :get (fn [req]
                          (if-not (authenticated? req)
                            (throw-unauthorized)
                            {:status 200
                             :headers {"Content-Type" "text/plain"}
                             :body "ok"}))}]])

   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404
                              :headers {"Content-Type" "text/plain; charset=utf-8"}
                              :body "Not Found"})}))))


(defonce stop-http (atom nil))

(defn start! []
  (let [port (Integer. (or (System/getenv "HTTP_PORT") 8080))]
    (println (str "Running HTTP server at localhost:" port))
    (reset! stop-http
            (http/run-server (middleware app) {:port port})))
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