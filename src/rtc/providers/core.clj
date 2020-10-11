(ns rtc.providers.core
  (:require
   [clojure.java.jdbc :as jdbc]
   [honeysql.core :as sql]
   [rtc.db :as db]
   [rtc.users.core :as u]))


(defn create! [provider]
  #_(jdbc/with-db-transaction
      [conn {:isolation :read-uncommitted}])
  (try
    (db/create-user! provider)
    (let [user (u/email->user (:email provider))]
      user
      #_(db/create-provider! (merge provider user))
      #_(db/get-provider {:id (:id user)}))
    (catch org.postgresql.util.PSQLException e
      {:error (.getMessage e)})))


(comment

  (create! {:email "rtc10@example.com" :pass "eyyy" :is_admin true :state "OR"})

  (try
    (db/create-user! {:email "me@example.com" :pass "password123" :is_admin true :state "WA"})
    (db/create-user! {:email "you@example.com" :pass "password234" :is_admin false})
    (db/create-user! {:email "them@example.com" :pass "password345" :is_admin false})
    (catch Exception e
      (.getMessage e)))

  (db/get-user {:id 1})
  (db/get-provider {:id 1})

  (try
    (db/create-provider! {:id 1 :email "foo" :state "WA"})
    (db/create-provider! {:id 2 :state "OR"})
    (catch Exception e
      (.getMessage e)))

  (db/get-provider {:id 1})
  (db/get-provider {:id 2}))