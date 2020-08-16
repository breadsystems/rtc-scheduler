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
   [rtc.api.core :as api]
   [rtc.assets.core :as assets]
   [rtc.auth.core :as auth]
   [rtc.db]
   [rtc.env :as env]
   [rtc.intake.core :as intake]
   [rtc.layout :as layout]
   [rtc.users.handlers :as user]))


;; TODO CSRF isn't working right now; figure out why
(defn- debugging-csrf-error-handler
  ([req]
   (layout/error-page {:err "Invalid CSRF Token!" :req (:session req "(nil)")}))
  ([_ req _]
   (debugging-csrf-error-handler req)))

(defn- csrf-middleware [handler]
  (wrap-anti-forgery handler {:error-handler debugging-csrf-error-handler}))


;; shared session store for all reitit routes
;; https://github.com/metosin/reitit/issues/205
(def store (memory/memory-store))


(def app
  (ring/ring-handler
   (ring/router
    [""
     ;; TODO figure out the right order for middleware & fix anti-CSRF
     {:middleware [wrap-params auth/wrap-identity #_csrf-middleware]}
     ["/" {:get (fn [_req]
                  (layout/markdown-page
                   {:file "home.md"
                    :after [:section.center.spacious
                            [:a.call-to-action {:href "/get-care"} "Get Care"]]}))}]

     ["/api/graphql" {:post (fn [req]
                              {:status 200
                               :headers {"Content-Type" "application/edn"}
                               :body (-> req :body slurp (api/q {:request req}))})}]

     (let [conf {:get intake/get-care-handler}]
       ["/get-care"
        ["" conf]
        ["/*" conf]])

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