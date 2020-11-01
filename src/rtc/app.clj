;; Central RTC application namespace, where main method lives.
;; This is the code that Java invokes on application startup.
(ns rtc.app
  (:require
   [clojure.string :as string]
   [config.core :as config]
   [mount.core :as mount :refer [defstate]]
   [org.httpkit.server :as http]
   [reitit.ring :as ring]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.memory :as memory]
   [rtc.api.core :as api] ;; TODO
   [rtc.rest.core :as rest]
   [rtc.assets.core :as assets]
   [rtc.auth.core :as auth]
   [rtc.db :as db]
   [rtc.env :as env]
   [rtc.intake.core :as intake]
   [rtc.layout :as layout]
   [rtc.users.handlers :as user]))


;; shared session store for all reitit routes
;; https://github.com/metosin/reitit/issues/205
(def store (memory/memory-store))


(def app
  (ring/ring-handler
   (ring/router
    [""
     {:middleware [wrap-params auth/wrap-identity wrap-anti-forgery]}
     ["/" {:get (fn [_req]
                  (layout/markdown-page
                   {:file "home.md"
                    :after [:section.center.spacious
                            ;; TODO get this from EDN
                            [:a.call-to-action {:href "/get-care"}
                             [:span {:data-lang "en"} "Get Care"]
                             [:span {:data-lang "es" :style {:display :none}} "Recibe Atencion Médica"]]]}))}]

     ;; TODO remove
     ["/api/graphql" {:post (fn [req]
                              {:status 200
                               :headers {"Content-Type" "application/edn"}
                               :body (-> req :body slurp (api/q {:request req}))})}]

     (rest/endpoints {:mount "/api/v1"})

     ["/get-care"
      ["" {:get intake/get-care-handler}]
      ["/*" {:get intake/get-care-handler}]]

     ["/register" user/register-handler]
     ["/login" auth/login-handler]
     ["/logout" auth/logout-handler]

     ;; Make sure any route matching /comrades/* serves the admin center.
     ;; From here on down, routing is done client-side.
     (let [conf {:middleware [auth/wrap-auth]
                 :get (fn [_req]
                        (layout/admin-page {:title "Comrades"}))}]
       ["/comrades"
        ["" conf]
        ["*" {:get (fn [{:keys [uri]}]
                     (if (string/ends-with? uri "/")
                       {:headers {"Location" (string/replace uri #"/$" "")}
                        :status 302}
                       (layout/admin-page {:title "Comrades"})))}]])])

   (ring/routes
    (assets/wrap-asset-headers (ring/create-resource-handler {:path "/"}))
    (ring/redirect-trailing-slash-handler {:method :strip})
    (ring/create-default-handler
     {:not-found (constantly {:status 404
                              :headers {"Content-Type" "text/plain; charset=utf-8"}
                              :body "Not Found"})}))))


(defonce stop-http (atom nil))

(defn start! []
  (let [port (Integer. (:port config/env 80))]
    (println (str "Running HTTP server at localhost:" port))
    (when (:dev-disable-auth config/env)
      (println "NOTICE: Authentication is disabled!"))
    (reset! stop-http
            (http/run-server (-> app
                                 (wrap-session)
                                 (wrap-params)
                                ;;  (wrap-anti-forgery)
                                 (env/middleware))
                             {:port port})))
  nil)

(defn stop! []
  (println "Stopping HTTP server")
  (when (fn? @stop-http)
    (@stop-http))
  (reset! stop-http nil))

(defstate http-server
  :start (start!)
  :stop  (stop!))


(defn -main [& _args]
  (mount/start))

(defn- restart! []
  (mount/stop)
  (mount/start))

(comment

  ;; Evaluate this to start the app in the REPL.
  (mount/start)

  ;; Restart the dev environment.
  (restart!)

  ;; Recreate the test admin user.
  (when-let [admin-uid (:id (db/get-user-by-email {:email "rtc@example.com"}))]
    (db/delete-user! {:id admin-uid})
    (restart!))

  )