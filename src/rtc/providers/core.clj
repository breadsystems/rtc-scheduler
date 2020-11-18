(ns rtc.providers.core
  (:require
   [buddy.hashers :as hash]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rtc.db :as db]))


(defn create! [provider]
  (-> (sqlh/insert-into :users)
      (sqlh/values [(assoc provider :is_provider true)])
      (sql/format)
      (db/execute!)))

(defn id->provider [id]
  {:pre [(int? id)]}
  (when-let [provider (first (db/query ["SELECT * FROM users WHERE is_provider = true AND id = ?" id]))]
    (dissoc provider :pass)))

(defn email->provider [email]
  {:pre [(string? email)]}
  (when-let [provider (first (db/query ["SELECT * FROM users WHERE is_provider = true AND email = ?" email]))]
    (dissoc provider :pass)))


(comment

  (create! {:email "rtc10@example.com" :pass (hash/derive "eyyy") :is_admin true :state "OR"})

  (id->provider 1) ;; user may exist, but not as a provider => nil
  (id->provider 2)
  (email->provider "coby02@cobytamayo.com")

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