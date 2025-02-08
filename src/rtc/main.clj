(ns rtc.main
  (:require
    [aero.core :as aero]
    [clojure.edn :as edn]
    [clojure.java.shell :as shell]
    [clojure.string :as string]
    [integrant.core :as ig]
    [org.httpkit.server :as http]
    [reitit.ring :as rr]
    [ring.middleware.defaults :as ring]

    [systems.bread.alpha.core :as bread]
    [systems.bread.alpha.defaults :as bread-defaults]
    [systems.bread.alpha.database :as db]
    [systems.bread.alpha.route :as route]
    [systems.bread.alpha.plugin.auth :as auth]
    [systems.bread.alpha.plugin.reitit]
    [systems.bread.alpha.plugin.datahike]
    [systems.bread.alpha.plugin.rum :as rum]
    [systems.bread.alpha.user :as user]

    [rtc.actions :as actions]
    [rtc.admin :as admin]
    [rtc.appointments :as appt]
    [rtc.env]
    [rtc.intake :as intake]
    [rtc.schema :as schema]
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

(defmethod bread/action ::auth
  [{:keys [session uri] :as req} {:keys [protected-prefixes]} _]
  (let [protected? (some (partial string/starts-with? uri) protected-prefixes)
        should-redirect? (bread/hook req ::auth/should-redirect?
                                     (and (nil? (:user session)) protected?))]
    (if should-redirect?
      (let [;; TODO URL-encode
            next-uri uri]
        {:headers {"Location" (format "/login?next=%s" next-uri)}
         :status 302})
      req)))

(defmethod ig/init-key :bread/app [_ app-config]
  (let [plugins (conj
                  (bread-defaults/plugins app-config)
                  (rum/plugin)
                  (auth/plugin {:auth/protected-prefixes #{"/admin"}
                                :auth/login-route "/login"})
                  {:hooks
                   {::user/pull
                    [{;; TODO update Bread
                      :action/name ::bread/value
                      :action/value [:db/id
                                     :thing/uuid
                                     :thing/slug
                                     :user/email
                                     :user/name
                                     :user/preferences
                                     {:user/roles [:role/key
                                                   {:role/abilities
                                                    [:ability/key]}]}]}]
                    ::bread/route
                    [;; TODO upstream auth & delete
                     {:action/name ::auth
                      :protected-prefixes #{"/admin"}
                      :action/description "Require login for /admin routes"}]
                    ::bread/expand
                    [{:action/name ::system
                      :action/description "Add system-global state to ::data"}
                     {:action/name ::now
                      :action/description "Add current datetime to ::data"}]
                    ::db/migrations
                    [{:action/name ::schema/migrations
                      :action/description "Add RTC-specific db migrations"}]}})]
    (bread/load-app (bread/app {:plugins plugins}))))

(defmethod ig/init-key :bread/handler [_ {:keys [loaded-app]}]
  (bread/handler loaded-app))

(defmethod ig/init-key :bread/db
  [_ {:keys [recreate? force?] :as db-config}]
  (db/create! db-config {:force? force?})
  (assoc db-config :db/connection (db/connect db-config)))

(defmethod ig/halt-key! :bread/db
  [_ {:keys [recreate?] :as db-config}]
  (when recreate? (db/delete! db-config)))


(defmethod ig/init-key :app/env [_ env]
  (or env :prod))

(defmethod ig/init-key :app/router [_ {:keys [authentication]}]
  (let [auth-enabled? (:enabled? authentication true)]
    ;; TODO trailing slash
    (rr/router
      [["/admin"
        [""
         {:get {:handler #'admin/show}}]
        ["/appointments"
         {:get {:handler {:dispatcher/type ::appt/show-all
                          :dispatcher/component #'appt/AppointmentsList}}}]
        ["/appointments/{thing/uuid}"
         ["" {:get {:handler {:dispatcher/type ::appt/by-uuid ;; TODO ::thing
                              :dispatcher/component #'appt/AppointmentPage}}}]
         ["/notes" {:post {:handler {:dispatcher/type ::appt/add-note}}}]]
        ["/providers"
         {:get {:handler #'admin/show-providers
                :middleware [#_(admin/wrap-filter-params {:query provider/filter-params})]}}]]
       ;; TODO AUTHENTICATION
       ["/login"
        {:dispatcher/type ::auth/login
         ;; TODO implement RTC LoginPage
         :dispatcher/component #'auth/login-page}]
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

(defn -wrap-dev-user [f]
  (fn [{:as req
        session :session}]
    (let [{:app/keys [authentication env]} @system
          session (cond
                    (:user session) session
                    (and (= :dev env) (:dev/user authentication))
                    {:user (:dev/user authentication)}
                    :else session)]
      (f (assoc req :session session)))))

(defn -wrap-try-catch [f]
  (fn [req]
    (try
      (f req)
      (catch Throwable e
        (let [data (dissoc (ex-data e) :app ::bread/core?)
              m (Throwable->map e)]
          {:body (str
                   (.getMessage e)
                   "<br><br>"
                   (with-out-str (clojure.pprint/pprint data))
                   "<br>"
                   "<h2>Trace:</h2>"
                   (apply str (map #(apply str % "<br>") (:trace m))))
           :status 500
           :headers {"Content-Type" "text/html"}})))))

(defmethod ig/init-key :bread/profilers [_ profilers]
  ;; Enable hook profiling.
  (alter-var-root #'bread/*profile-hooks* (constantly true))
  (map
    (fn [{h :hook act :action/name f :f :as profiler}]
      (let [tap (bread/add-profiler
                  (fn [{{:keys [action hook] :as invocation} ::bread/profile}]
                    (if (and (or (nil? (seq h)) ((set h) hook))
                             (or (nil? (seq act)) ((set act) (:action/name action))))
                      (f invocation))))]
        (assoc profiler :tap tap)))
    profilers))

(defmethod ig/halt-key! :bread/profilers [_ profilers]
  (doseq [{:keys [tap]} profilers]
    (remove-tap tap)))

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
                    -wrap-dev-user
                    -wrap-try-catch
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

(defn log-dispatch [{:keys [hook action result]}]
  (prn hook '=> (::bread/dispatcher result)))
(defn log-expand [{:keys [hook action result]}]
  (prn hook (:action/name action) '===>)
  (clojure.pprint/pprint (::bread/expansions result)))
(defn log-request [{req :result}]
  (let [req-keys (filter #(not= "systems.bread.alpha.core" (namespace %)) (keys req))]
    (clojure.pprint/pprint (select-keys req req-keys))))
(defn log-flash [{:keys [result]}]
  (prn :flash (:flash result)))

(defn start! [config]
  (let [config (assoc config
                      :app/initial-config config
                      :bread/app {:db (ig/ref :bread/db)
                                  :routes {:router (ig/ref :app/router)}
                                  :i18n {:query-strings? false}}
                      :bread/handler {:loaded-app (ig/ref :bread/app)}
                      :bread/profilers [#_{:hook #{::bread/dispatch} :f #'log-dispatch}
                                        #_{:hook #{::bread/expand} :f #'log-expand}
                                        {:hook #{::bread/request} :f #'log-request}
                                        {:hook #{::bread/response} :f #'log-flash}]
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

;; TODO ship bread.tools
(defn diagnose-expansions [app req]
  (let [app (-> (merge app req)
                (bread/hook ::bread/route)
                (bread/hook ::bread/dispatch))
        expansions (::bread/expansions app)
        {:keys [data err n before]}
        (reduce (fn [{:keys [data n]} _]
                  (try
                    (let [before data
                          data
                          (-> app
                              ;; Expand n expansions
                              (assoc ::bread/expansions
                                     (subvec expansions 0 (inc n)))
                              (bread/hook ::bread/expand)
                              ::bread/data)]
                      {:data data :n (inc n) :data-before before})
                    (catch Throwable err
                      (reduced {:err err :n n}))))
                {:data {} :err nil :n 0} expansions)]
    (if err
      {:err err
       :at n
       :query (get-in app [::bread/expansions n])
       :before before}
      {:ok data})))

(comment

  (restart! (-> "resources/dev.edn" aero/read-config))
  (stop! (-> "resources/dev.edn" aero/read-config))

  (deref system)
  (:bread/handler @system)
  (::bread/plugins (:bread/app @system))
  (::bread/hooks (:bread/app @system))
  (= (route/router (:bread/app @system))
     (bread/hook (:bread/app @system) ::route/router))

  (diagnose-expansions (:bread/app @system) {:uri "/admin/appointments/123"
                                             :request-method :get})

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

  (db/q (db/database (:bread/app @system))
        '{:find [(pull ?e [* {:thing/fields [*]}])]
          :where [[?e :appt/name]
                  [?e :appt/status :waiting]]})

  (as-> (:bread/app @system) $
    (assoc $ :uri "/admin/appointments" :request-method :get)
    (bread/route-dispatcher (route/router $) $))
  (as-> (:bread/app @system) $
    (assoc $
           :uri "/admin/appointments/b358445e-aa6e-44a0-bf48-dafa93ffa446"
           :request-method :get
           :session {:user {:user/name "Test"}})
    (bread/hook $ ::bread/route)
    (bread/hook $ ::bread/dispatch)
    (::bread/expansions $)
    #_
    (bread/hook $ ::bread/render))

  ((:bread/handler @system) {:uri "/admin/appointments" :request-method :get})
  (::bread/expansions ((:bread/handler @system)
                       {:uri "/admin/appointments/123"
                        :request-method :get
                        :session {:user {:user/name "Test"}}}))
  (def $req {:uri "/admin/appointments/123"
             :request-method :get
             :session {:user {:user/name "Test"}}})
  (-> (:bread/app @system)
      (merge $req)
      (bread/hook ::bread/route)
      (bread/hook ::bread/dispatch))

  (require '[systems.bread.alpha.util.datalog :as datalog])
  (reduce
    (fn [migrations {:migration/keys [key attrs]}]
      (assoc migrations key (map :db/ident attrs)))
    {}
    (datalog/migrations (db/database (:bread/app @system))))

  ;;

  )

(defn -main [& file]
  (-> file aero/read-config start!))
