(ns rtc.main
  (:require
    [aero.core :as aero]
    [clojure.edn :as edn]
    [integrant.core :as ig]
    [org.httpkit.server :as http]
    [reitit.ring :as rr]
    [ring.middleware.defaults :as ring]

    [rtc.actions :as actions]
    [rtc.admin :as admin]
    [rtc.appointments :as appt]
    [rtc.auth :as auth]
    [rtc.intake :as intake]
    [rtc.ui :as ui])
  (:import
    [java.time LocalDateTime]))

;; CONFIG

(declare system)

(defmethod ig/init-key :app/router [_ {:keys [authentication]}]
  (let [auth-enabled? (:enabled? authentication)]
    (rr/router
      [["/admin"
        {:get {:middleware [(when auth-enabled? auth/wrap-require-auth)]}}
        [""
         {:get {:handler #'admin/show}}]
        ["/appointments"
         {:get {:handler #'appt/show-all
                :middleware [(admin/wrap-filter-params {:query appt/filter-coercions})]}}]
        ["/appointments/{appt/uuid}"
         {:get {:handler #'appt/show}}]
        ["/providers"
         {:get {:handler #'admin/show-providers
                :middleware [#_(admin/wrap-filter-params {:query provider/filter-params})]}}]]
       ["/login"
        {:get {:handler #'auth/show-login}}]
       ["/get-care"
        {:get {:handler #'intake/show}}]])))

(defn -wrap-system [f]
  (fn [req]
    (f (assoc req :system @system))))

(defn -wrap-now [f]
  (fn [req]
    (f (assoc req :now (LocalDateTime/now)))))

(defn -wrap-keyword-headers [f]
  (fn [req]
    (let [res (f req)]
      (update res :headers clojure.walk/stringify-keys))))

(defn -wrap-default-content-type [f]
  (fn [req]
    (let [res (f req)]
      (update res :headers #(merge {:content-type "text/html"} %)))))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defmethod ig/init-key :app/authentication [_ auth-config]
  (merge {:enabled? true} auth-config))

(defmethod ig/init-key :clojure-version [_ _]
  (clojure-version))

(defmethod ig/init-key :started-at [_ _]
  (LocalDateTime/now))

(defmethod ig/init-key :initial-config [_ config]
  config)

(defmethod ig/init-key :app/http [_ {:keys [port ring-defaults router]
                                 :or {ring-defaults {}}}]
  ;; TODO timbre
  (println "Starting HTTP server on port" port)
  (let [wrap-config (as-> ring/secure-site-defaults $
                        (reduce #(assoc-in %1 (key %2) (val %2)) $ ring-defaults)
                        (assoc-in $ [:params :keywordize] true))
        handler (-> router
                    (rr/ring-handler (rr/create-default-handler {:not-found ui/NotFoundPage}))
                    -wrap-system
                    -wrap-default-content-type
                    -wrap-keyword-headers
                    -wrap-now
                    ui/wrap-rum-html
                    (ring/wrap-defaults wrap-config))]
    (http/run-server handler {:port port})))

(defmethod ig/halt-key! :app/http [_ stop-server]
  (when-let [prom (stop-server :timeout 100)]
    @prom))

(defn websocket-handler [{:keys [authentication]}]
  (fn [req]
    (if (or (false? (:enabled? authentication)) (get-in req [:session :user]))
      (http/with-channel req chan
        (http/on-close chan (fn [status]
                              (println "channel closed:" status)))
        (http/on-receive chan (fn [data]
                                (let [action (edn/read-string data)]
                                  (actions/act! action chan)))))
      {:status 401
       :body "Not authorized"})))

(defmethod ig/init-key :app/websocket [_ {:keys [port authentication]}]
  (http/run-server (websocket-handler {:authentication authentication})
                   {:port port}))

(defmethod ig/halt-key! :app/websocket [_ stop-server]
  (when-let [prom (stop-server :timeout 100)]
    @prom))

;; RUNTIME

(defonce system (atom nil))

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
  (stop! (-> "resources/dev.edn" aero/read-config))

  (deref system)
  ((rr/ring-handler router) {:uri "/admin"
                             :request-method :get})

  ;;

  )

(defn -main [& file]
  (-> file aero/read-config start!))
