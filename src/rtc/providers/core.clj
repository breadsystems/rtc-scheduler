(ns rtc.providers.core
  (:require
   [rtc.db :as db]))




(comment

  (try
    (db/create-user! {:email "me@example.com" :pass "password123"})
    (db/create-user! {:email "you@example.com" :pass "password234"})
    (db/create-user! {:email "them@example.com" :pass "password345"})
    (catch Exception e
      (.getMessage e)))

  (db/get-user {:id 1})

  (try
    (db/create-provider! {:id 1 :state "WA"})
    (db/create-provider! {:id 2 :state "OR"})
    (catch Exception e
      (.getMessage e)))

  (db/get-provider {:id 1})
  (db/get-provider {:id 2}))