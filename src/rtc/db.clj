(ns rtc.db
  (:require
   [cheshire.core :as cheshire]
   [config.core :as config :refer [env]]
   [conman.core :as conman]
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as string]
   [migratus.core :as migratus]
   [mount.core :as mount :refer [defstate]])
  (:import
   [org.postgresql.util PGobject]
   [java.net URI]))


(defn- ->jdbc-url [database-url]
  (if (string/starts-with? database-url "postgres://")
    (let [uri (URI. database-url)
          user-info (.getUserInfo uri)
          [username password] (when user-info (.split user-info ":"))
          creds (when user-info
                  (str "?user=" username "&password=" password))]
      (str "jdbc:postgresql://" (.getHost uri) ":" (.getPort uri) (.getPath uri) creds))
    database-url))

;; Parse the DATABASE_URL environment variable into something JDBC can use
;; https://devcenter.heroku.com/articles/connecting-to-relational-databases-on-heroku-with-java#using-the-database_url-in-plain-jdbc
(defstate url
  :start (if-let [database-url (:database-url env)]
           (->jdbc-url database-url)
           (throw (ex-info "No database-url detected!" {:causes #{:no-database-url}}))))

(defstate ^:dynamic *db*
  :start (do
           (println "Connecting to database at URL:" url)
           (conman/connect! {:jdbc-url url}))
  :stop  (conman/disconnect! *db*))

(defn bind! []
  (conman/bind-connection
   *db*
   "sql/util.sql"
   "sql/rtc-base.sql"))

(defn query
  "Run an arbitrary SQL query"
  [sql]
  (jdbc/query {:connection-uri url} sql))

(defn execute!
  "Run an effectful SQL query, such as a DELETE or INSERT"
  ([sql]
   (jdbc/execute! {:connection-uri url} sql))
  ([sql opts]
   (jdbc/execute! {:connection-uri url} sql opts)))

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

  (query "SELECT * FROM appointments")

  (get-invitations {:redeemed false :invited_by 1}))


(defn migration-config []
  {:store :database
   :migration-dir "migrations/"
   :db {:datasource *db*}})

(defstate migrations
  :start (do
           (println "Performing database migrations...")
           (migratus/init (migration-config))
           (migratus/migrate (migration-config))
           (println "Done.")))

(defn reset-everything!! []
  (try
    (reconnect!)
    (migratus/rollback (migration-config))
    (mount/stop #'migrations)
    (mount/start #'migrations)
    true
    (catch Exception e
      (.getMessage e))))


(comment
  (reset-everything!!)

  ;; create a migration
  (migratus/create (migration-config) "migration-name-here")

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