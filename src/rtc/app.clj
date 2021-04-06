;; Central RTC application namespace, where main method lives.
;; This is the code that Java invokes on application startup.
(ns rtc.app
  (:require
   [clojure.string :as string]
   [mount.core :as mount :refer [defstate]]
   [org.httpkit.server :as http]
   [reitit.ring :as ring]
   [ring.middleware.anti-forgery :as anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.memory :as memory]
   [rtc.rest.core :as rest]
   [rtc.assets.core :as assets]
   [rtc.auth.core :as auth]
   [rtc.db :as db]
   [rtc.env :refer [env]]
   [rtc.intake.core :as intake]
   [rtc.layout :as layout]
   [rtc.users.core :as u]
   [rtc.users.handlers :as user]))


;; shared session store for all reitit routes
;; https://github.com/metosin/reitit/issues/205
(def store (memory/memory-store))


(def app
  (ring/ring-handler
   (ring/router
    [""
     ["/" {:get (fn [_req]
                  (layout/markdown-page
                   {:file "home.md"
                    :before [:section.center.spacious
                            ;; TODO get this from EDN
                            [:a.call-to-action {:href "/get-care"}
                             [:span {:data-lang "en"} "Get Care"]
                             [:span {:data-lang "es" :style {:display :none}} "Recibe Atencion Médica"]]]
                    :after [:section.center.spacious
                            ;; TODO get this from EDN
                            [:a.call-to-action {:href "/get-care"}
                             [:span {:data-lang "en"} "Get Care"]
                             [:span {:data-lang "es" :style {:display :none}} "Recibe Atencion Médica"]]]}))}]

     (rest/endpoints {:mount "/api/v1"})

     ["/get-care"
      ["" {:get intake/get-care-handler}]
      ["/*" {:get intake/get-care-handler}]]

     ["/register" user/register-handler]
     ["/login" auth/login-handler]
     ["/logout" auth/logout-handler]

     ;; Make sure any route matching /comrades/* serves the admin center.
     ;; From here on down, routing is done client-side.
     ["/comrades"
      ["" {:middleware [auth/wrap-auth]
           :get (fn [_req]
                  (layout/admin-page {:title "Comrades"}))}]
      ["*" {:middleware [auth/wrap-auth]
            :get (fn [{:keys [uri]}]
                   (if (string/ends-with? uri "/")
                     {:headers {"Location" (string/replace uri #"/$" "")}
                      :status 302}
                     (layout/admin-page {:title "Comrades"})))}]]])

   (ring/routes
    (assets/wrap-asset-headers (ring/create-resource-handler {:path "/"}))
    (ring/redirect-trailing-slash-handler {:method :strip})
    (ring/create-default-handler
     {:not-found (constantly {:status 404
                              :headers {"Content-Type" "text/plain; charset=utf-8"}
                              :body "Not Found"})}))))


(defonce stop-http (atom nil))

(defn- read-token [{:keys [params headers]}]
  (or (:__anti-forgery-token params)
      (get headers "x-csrf-token")))

(defn- wrap-https
  "Redirect to HTTPS in production"
  [handler]
  (fn [req]
    (if (and (:https-dest env) (= :http (:scheme req)))
      {:status 302
       :headers {"Location" (:https-dest env)}}
      (handler req))))

(defn- wrap-dev-identity
  "Load default dev admin user into the session identity when auth is explicitly disabled.
   Note that the wrap-identity middleware loads the actual :identity key into req."
  [handler]
  (if (:dev-disable-auth env)
    (fn [req]
      (handler (assoc-in req [:session :identity] auth/default-user)))
    handler))

(defn- env-anti-forgery
  "Wrap handler in anti-forgery middleware unless explicitly disabled."
  [handler]
  (when (:dev-disable-anti-forgery env)
    (println "NOTICE: Anti-forgery protection is disabled!"))
  (if-not (:dev-disable-anti-forgery env)
    (wrap-anti-forgery handler {:read-token read-token})
    handler))

(defn start! []
  (let [port (Integer. (:port env 80))]
    (println (str "Running HTTP server at localhost:" port))
    (when (:dev-disable-auth env)
      (println "NOTICE: Authentication is disabled!"))
    (reset! stop-http
            (http/run-server (-> app
                                 (env-anti-forgery)
                                 (auth/wrap-identity)
                                 (wrap-dev-identity)
                                 (wrap-session)
                                 (wrap-keyword-params)
                                 (wrap-params)
                                 (wrap-https)
                                 (rtc.env/middleware))
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

  ;; Re-parse SQL helper files
  (db/bind!)

  ;; Restart the dev environment.
  (restart!)

  ;; Check environment variables.
  (:dev-disable-auth env)
  (:dev-disable-anti-forgery env)
  (:default-authy-user-id env)
  (:authy-api-key env)

  auth/default-user

  ;; Recreate the test admin user.
  (do
    (when-let [admin-uid (:id (u/email->user (:default-admin-email env)))]
      (db/delete-user! {:id admin-uid}))
    (restart!))

  ;;
  )
