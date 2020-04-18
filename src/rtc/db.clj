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
                        "sql/util.sql"
                        "sql/rtc-base.sql")

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
  ;; => {:email "newaddr@earthseed.com", :first_name "Octavia", :phone "2535551234", :pronouns "she/her", :state "WA", :ok_to_text false, :alias "George Simcoff", :id 1, :date_modified #inst "2020-04-18T19:04:35.019280000-00:00", :last_name "Butler", :date_created #inst "2020-04-18T18:59:46.058771000-00:00"}

  (get-careseeker {:id 2})
  (get-careseeker {:id 3})
  
  (delete-careseeker! {:id 3})
  
  (update-careseeker! {:id 1
                       :first-name "Octavia"
                       :last-name "Butler"
                       :email "newaddr@earthseed.com"
                       :pronouns "she/her"
                       :phone "2535551234"
                       :ok-to-text? false
                       :state "WA"}))


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
    (mount/start #'migrations)
    (catch Exception e
      (.getMessage e)))
  (mount/stop #'migrations)
  (migratus/rollback migration-config)

  ;; create a migration
  (migratus/create migration-config "migration-name-here")

  ;; list all schema migrations
  (get-migrations))