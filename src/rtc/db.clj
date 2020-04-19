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

(defn bind! []
  (conman/bind-connection
   *db*
   "sql/util.sql"
   "sql/rtc-base.sql"))

(bind!)


(comment
  ;; connect/disconnect/reconnect database
  (mount/stop #'*db*)
  (mount/start #'*db*)
  (reconnect!)

  ;; run HugSQL-generated fns
  (try
    (create-user! {:email "me@example.com" :pass "password123"})
    (create-user! {:email "you@example.com" :pass "password234"})
    (create-user! {:email "them@example.com" :pass "password345"})
    (catch Exception e
      (.getMessage e)))

  (get-user {:id 1})

  (try
    (create-careseeker! {:email "octaviabutler@earthseed.com" :alias "George Simcoff" :state "WA"})
    (create-careseeker! {:email "shevek@annarres.net" :alias "Selma Blaise" :state "OR"})
    (create-careseeker! {:email "beloved@morrison.email" :alias "Alan McLoughlin" :state "CA"})
    (catch Exception e
      (.getMessage e)))

  (get-careseeker {:id 1})
  (get-careseeker {:id 2})
  (get-careseeker {:id 3})

  (delete-careseeker! {:id 3})
  (get-careseeker {:id 3})

  (update-careseeker! {:id 1
                       :first-name "Octavia"
                       :last-name "Butler"
                       :email "newaddr@earthseed.com"
                       :pronouns "she/her"
                       :phone "2535551234"
                       :ok-to-text? false
                       :state "WA"})

  (try
    (create-provider! {:user-id 1 :state "WA"})
    (create-provider! {:user-id 2 :state "OR"})
    (catch Exception e
      (.getMessage e)))

  (get-provider {:user-id 1})
  (get-provider {:user-id 2}))


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
  ;; manage migrations
  (try
    (migratus/rollback migration-config)
    (mount/stop #'migrations)
    (mount/start #'migrations)
    true
    (catch Exception e
      (.getMessage e)))

  ;; create a migration
  (migratus/create migration-config "migration-name-here")

  ;; list all schema migrations
  (get-migrations))