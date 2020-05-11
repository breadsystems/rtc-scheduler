(ns rtc.db
  (:require
   [cheshire.core :as cheshire]
   [conman.core :as conman]
   [migratus.core :as migratus]
   [mount.core :as mount :refer [defstate]])
  (:import (org.postgresql.util PGobject)))


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

(defn bind! []
  (conman/bind-connection
   *db*
   "sql/util.sql"
   "sql/rtc-base.sql"))

(bind!)

(defn reconnect! []
  (mount/stop #'*db*)
  (mount/start #'*db*)
  (bind!)
  nil)


(comment
  ;; connect/disconnect/reconnect database
  (mount/stop #'*db*)
  (mount/start #'*db*)
  (reconnect!)

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
)


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


;; Handle PostgresQL data types polymorphically
;; Lifted from Luminus template

(defn pgobj->clj [^org.postgresql.util.PGobject pgobj]
  (let [type (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json" (cheshire/parse-string value true)
      "jsonb" (cheshire/parse-string value true)
      "citext" (str value)
      value)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))

  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))

  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))

  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))

  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (cheshire/generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))

  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))