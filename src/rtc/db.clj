(ns rtc.db
  (:require
   [conman.core :as conman]
   [migratus.core :as migratus]
   [mount.core :as mount :refer [defstate]]))


(defn connect! []
  (if-let [database-url (System/getenv "DATABASE_URL")]
    (do
      (println "Connecting to database at URL:" database-url)
      (conman/connect! {:jdbc-url database-url}))
    (throw
     (ex-info "No DATABASE_URL environment detected!"
              {:causes #{:no-database-url}}))))

(defstate ^:dynamic *db*
  :start (connect!)
  :stop  (conman/disconnect! *db*))

(defn reconnect! []
  (mount/stop #'*db*)
  (mount/start #'*db*)
  nil)

(conman/bind-connection *db*
                        "sql/users.sql"
                        "sql/util.sql")


(def migration-config {:store :database
                       :migration-dir "migrations/"
                       :db {:classname "org.postgresql.Driver"
                            :subprotocol "postgresql"
                            :subname (or (System/getenv "DATABASE_NAME") "rtc")}})

(defstate migrations
  :start (do
           (migratus/init migration-config)
           (migratus/migrate migration-config)))


(comment
  (mount/stop #'*db*)
  (mount/start #'*db*)
  (mount/start #'migrations)
  (mount/stop #'migrations)
  (reconnect!)

  (try
    (create-user! {:email "me@example.com" :pass "password123"})
    (create-user! {:email "you@example.com" :pass "password234"})
    (create-user! {:email "them@example.com" :pass "password345"})
    (catch Exception e
      (println "Exception:" (.getMessage e))
      (.getMessage e)))

  (get-user {:id 1})
  
  (get-migrations))