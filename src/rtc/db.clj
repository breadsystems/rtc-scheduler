(ns rtc.db
  (:require
   [conman.core :as conman]
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


(conman/bind-connection *db* "sql/users.sql")


(comment
  (mount/stop #'*db*)
  (mount/start #'*db*)
  (reconnect!))