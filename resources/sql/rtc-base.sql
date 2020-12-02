-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users (email, pass, first_name, last_name, pronouns, phone, state,
last_login, date_created, date_modified, is_admin, authy_id, preferences)
VALUES (:email, :pass, :first_name, :last_name, :pronouns, :phone, :state,
NOW(), NOW(), NOW(), :is_admin, :authy_id, :preferences)

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users WHERE id = :id

-- :name get-all-users :? :*
-- :doc retrieves useful fields for all users in the database
SELECT id, email, first_name, last_name, pronouns, phone, state, is_admin, is_provider
FROM users

-- :name get-all-providers :? :*
-- :doc retrieves common fields for all users in the database
SELECT id, email, first_name, last_name, pronouns, phone, state, is_admin
FROM users
WHERE is_provider = true

-- :name get-user-by-email :? :1
-- :doc retrieves a user record given an email
SELECT u.* FROM users u WHERE email = :email

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users WHERE id = :id


-- :name get-invitation :? :1
-- :doc get an invitation by email and code
SELECT * FROM invitations WHERE email = :email
AND code = :code AND redeemed = false AND now() < (date_invited + interval '72 hours')

-- :name create-invitation! :! :n
-- :doc creates a new invitation record
INSERT INTO invitations (email, code, date_invited, invited_by, redeemed)
VALUES (:email, :code, now(), :invited_by, false)

-- :name redeem-invitation! :! :n
-- :doc redeem an existing email/code invitation combo
UPDATE invitations SET redeemed = true
WHERE email = :email AND code = :code AND now() < (date_invited + interval '72 hours')

-- :name create-appointment-need! :! :n
-- :doc create an appointment_need record
INSERT INTO appointment_needs (need_id, appointment_id, info)
VALUES (:need-id, :appointment-id, :info)

-- :name update-appointment-need! :! :n
-- :doc update an existing appointment_need record
UPDATE appointment_needs SET info = :info, contact_id = :contact-id
WHERE need_id = :need-id AND appointment_id = :appointment-id

-- :name get-appointment :? :1
-- :doc get an appointment by its id
SELECT id, start_time, end_time, careseeker_id, provider_id FROM appointments WHERE id = :id


-- :name create-need! :! :n
-- :doc create a need record
INSERT INTO needs (name, description) VALUES (:name, :description)

-- :name update-need! :! :n
-- :doc update an existing need record
UPDATE needs SET name = :name, description = :description WHERE id = :id

-- :name get-need :? :1
-- :doc get an availability by its id
SELECT * FROM needs WHERE id = :id

-- :name get-needs :*
-- :doc list all needs
SELECT * FROM needs

-- :name delete-need :! :n
-- :doc delete an existing need record by its id
DELETE FROM needs WHERE id = :id