-- :name create-careseeker! :! :n
-- :doc create a new careseeker record
INSERT INTO careseekers (email, alias, state, date_created, date_modified)
VALUES (:email, :alias, :state, now(), now())

-- :name update-careseeker! :! :n
-- :doc update an existing careseeker record
UPDATE careseekers
SET first_name = :first-name, last_name = :last-name, email = :email, pronouns = :pronouns,
phone = :phone, ok_to_text = :ok-to-text?, state = :state, date_modified = now()
WHERE id = :id

-- :name get-careseeker :? :1
-- :doc retrieve a careseeker by their id
SELECT * FROM careseekers WHERE id = :id

-- :name delete-careseeker! :! :n
-- :doc delete a careseeker record given the id
DELETE FROM careseekers WHERE id = :id