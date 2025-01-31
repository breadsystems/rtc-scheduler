(ns rtc.main
  (:require
    [aero.core :as aero]
    [clojure.edn :as edn]
    [clojure.java.shell :as shell]
    [integrant.core :as ig]
    [org.httpkit.server :as http]
    [reitit.ring :as rr]
    [ring.middleware.defaults :as ring]

    [systems.bread.alpha.core :as bread]
    [systems.bread.alpha.defaults :as bread-defaults]
    [systems.bread.alpha.database :as db]
    [systems.bread.alpha.route :as route]
    [systems.bread.alpha.plugin.auth :as bread-auth]
    [systems.bread.alpha.plugin.reitit]
    [systems.bread.alpha.plugin.datahike]
    [systems.bread.alpha.plugin.rum :as rum]

    [rtc.actions :as actions]
    [rtc.admin :as admin]
    [rtc.appointments :as appt]
    [rtc.auth :as auth]
    [rtc.intake :as intake]
    [rtc.ui :as ui])
  (:import
    [java.time LocalDateTime]
    [java.util Date]))

;; CONFIG

(declare system)

(defmethod bread/action ::system [req _ _]
  (assoc-in req [::bread/data :system] @system))

(defmethod bread/action ::now [req _ _]
  (assoc-in req [::bread/data :now] (LocalDateTime/now)))

(defmethod ig/init-key :bread/app [_ app-config]
  (let [plugins (conj
                  (bread-defaults/plugins app-config)
                  (rum/plugin)
                  (bread-auth/plugin)
                  {:hooks
                   {::bread/dispatch
                    [{:action/name ::system
                      :action/description "Add system-global state to data"}
                     {:action/name ::now
                      :action/description "Add current datetime to data"}
                     ]}})]
    (bread/load-app (bread/app {:plugins plugins}))))

(defmethod ig/init-key :bread/handler [_ {:keys [loaded-app]}]
  (bread/handler loaded-app))

(defmethod ig/init-key :bread/db [_ db-config] db-config)

(defmethod ig/init-key :app/env [_ env]
  (or env :prod))

(defmethod ig/init-key :app/router [_ {:keys [authentication]}]
  (let [auth-enabled? (:enabled? authentication true)]
    ;; TODO trailing slash
    (rr/router
      [["/admin"
        {:get {:middleware [(when auth-enabled? auth/wrap-require-auth)]}}
        [""
         {:get {:handler #'admin/show}}]
        ["/appointments"
         {:get {:handler #'appt/show-all
                :middleware [(admin/wrap-filter-params {:query appt/filter-coercions})]}}]
        ["/appointments-test"
         {:get {:handler {:dispatcher/type ::appt/show-all
                          :dispatcher/component #'appt/AppointmentsList}}}]
        ["/appointments/{appt/uuid}"
         {:get {:handler #'appt/show}}]
        ["/providers"
         {:get {:handler #'admin/show-providers
                :middleware [#_(admin/wrap-filter-params {:query provider/filter-params})]}}]]
       ;; TODO AUTHENTICATION
       ["/login"
        {:get {:handler #'auth/show-login}
         #_#_
         :post {:handler #'auth/login}}]
       #_
       ["/logout"
        {:post {:handler #'auth/logout}}]
       ["/get-care"
        {:get {:handler #'intake/show}}]])))

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

(defmethod ig/init-key :app/clojure-version [_ _]
  (clojure-version))

(defmethod ig/init-key :app/started-at [_ _]
  (Date.))

(defmethod ig/init-key :app/git-hash [_ _]
  (-> (shell/sh "git" "rev-parse" "HEAD")
      :out str (subs 0 8)))

(defmethod ig/init-key :app/initial-config [_ config]
  config)

(defmethod ig/init-key :app/http [_ {:as config
                                     :keys [port ring-defaults]
                                     :or {ring-defaults {}}}]
  ;; TODO timbre
  (println "Starting HTTP server on port" port)
  (let [wrap-config (as-> ring/secure-site-defaults $
                      (reduce #(assoc-in %1 (key %2) (val %2)) $ ring-defaults)
                      (assoc-in $ [:params :keywordize] true))
        handler (-> (:bread/handler config)
                    -wrap-default-content-type
                    -wrap-keyword-headers
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
                      :app/initial-config config
                      :bread/app {:db (ig/ref :bread/db) ;; TODO
                                  :routes {:router (ig/ref :app/router)}}
                      :bread/handler {:loaded-app (ig/ref :bread/app)}
                      :app/started-at nil
                      :app/clojure-version nil
                      :app/git-hash nil)]
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
  (:bread/handler @system)
  (::bread/plugins (:bread/app @system))
  (::bread/hooks (:bread/app @system))
  (= (route/router (:bread/app @system))
     (bread/hook (:bread/app @system) ::route/router))

  (db/create! (:bread/db @system))
  (db/connect (:bread/db @system))
  (let [db @(db/connect (:bread/db @system))]
    (db/q db
          '{:find [?i]
            :in [$]
            :where [[_ :db/ident ?i]]}))
  (::bread/config (:bread/app @system))
  (db/connection (:bread/app @system))
  (bread/config (:bread/app @system) :db/connection)
  (db/database (:bread/app @system))

  (as-> (:bread/app @system) $
    (assoc $ :uri "/admin/appointments-test" :request-method :get)
    (bread/route-dispatcher (route/router $) $))
  (as-> (:bread/app @system) $
    (assoc $ :uri "/admin/appointments-test" :request-method :get)
    (bread/hook $ ::bread/route)
    (bread/hook $ ::bread/dispatch)
    (::bread/dispatcher $)
    #_#_
    (bread/hook $ ::bread/expand)
    (bread/hook $ ::bread/render))

  ((:bread/handler @system) {:uri "/admin/appointments" :request-method :get})
  (::bread/expansions ((:bread/handler @system) {:uri "/admin/appointments-test" :request-method :get}))

  ;;

  )

(defn -main [& file]
  (-> file aero/read-config start!))
