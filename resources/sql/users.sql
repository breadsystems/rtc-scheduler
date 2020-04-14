-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users (id, email, pass) VALUES (:id, :email, :pass)