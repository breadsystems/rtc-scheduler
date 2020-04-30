-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users (email, pass) VALUES (:email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users SET email = :email WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users WHERE id = :id

-- :name get-user-by-email :? :1
-- :doc retrieves a user record given an email
SELECT u.*, p.*, p.id != NULL AS is_provider FROM users u
LEFT JOIN providers p ON u.ID = p.ID
WHERE email = :email

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users WHERE id = :id


-- :name create-careseeker! :! :n
-- :doc create a new careseeker record
INSERT INTO careseekers (email, alias, state, date_created, date_modified)
VALUES (:email, :alias, :state, now(), now())

-- :name update-careseeker! :! :n
-- :doc update an existing careseeker record
UPDATE careseekers
SET first_name = :first-name,
  last_name = :last-name,
  email = :email,
  pronouns = :pronouns,
  phone = :phone,
  ok_to_text = :ok-to-text?,
  state = :state,
  date_modified = now()
WHERE id = :id

-- :name get-careseeker :? :1
-- :doc retrieve a careseeker by their id
SELECT id, alias, email FROM careseekers WHERE id = :id

-- :name delete-careseeker! :! :n
-- :doc delete a careseeker record given the id
DELETE FROM careseekers WHERE id = :id


-- :name create-provider! :! :n
-- :doc create a new provider record
INSERT INTO providers (state, id, date_created, date_modified)
VALUES (:state, :id, now(), now())

-- :name update-provider! :! :n
-- :doc update an existing provider record
UPDATE providers SET state = :state, date_modified = now() WHERE id = :id

-- :name get-provider :? :1
-- :doc retrieve a provider by their id
SELECT providers.id, state, email, first_name, last_name, pronouns, phone
FROM providers JOIN users ON (providers.id = users.id) WHERE providers.id = :id

-- :name delete-provider! :! :n
-- :doc delete a provider record given the id
DELETE FROM providers WHERE id = :id


-- :name create-appointment! :! :n
-- :doc create an appointment record
INSERT INTO appointments (start_time, end_time, careseeker_id, provider_id, reason)
VALUES (:start, :end, :careseeker-id, :provider-id, :reason)

-- :name create-appointment-need! :! :n
-- :doc create an appointment_need record
INSERT INTO appointment_needs (need_id, appointment_id, info)
VALUES (:need-id, :appointment-id, :info)

-- :name update-appointment! :! :n
-- :doc update an existing appointment record
UPDATE appointments
SET start_time = :start,
  end_time = :end,
  careseeker_id = :careseeker-id,
  provider_id = :provider-id,
  reason = :reason,
  provider_notes = :provider-notes,
  transcription = :transcription,
  state = :state,
  category = :category,
  resolution = :resolution
WHERE id = :id

-- :name update-appointment-need! :! :n
-- :doc update an existing appointment_need record
UPDATE appointment_needs SET info = :info, contact_id = :contact-id
WHERE need_id = :need-id AND appointment_id = :appointment-id

-- :name get-appointment :? :1
-- :doc get an appointment by its id
SELECT id, start_time, end_time, careseeker_id, provider_id FROM appointments WHERE id = :id

-- :name get-appointment-need :n
-- :doc get an appointment_need by its need/appointment ids
SELECT need_id, appointment_id, info, contact_id,
appt.contact_id, c.full_name, c.company_name, c.title, c.phone, c.email
FROM appointment_needs AS appt
LEFT JOIN contacts c ON c.id = appt.contact_id
WHERE appointment_id = :appointment-id

-- :name delete-appointment! :! :n
-- :doc delete an existing appointment record by its id
DELETE FROM appointments WHERE id = :id

-- :name delete-appointment-need! :! :n
-- :doc delete an existing appointment_need record by its id
DELETE FROM appointment_needs WHERE need_id = :need-id AND appointment_id = :appointment-id


-- :name create-availability! :! :n
-- :doc create a provider availability record
INSERT INTO availabilities (start_time, end_time, provider_id) VALUES (:start, :end, :provider-id)

-- :name update-availability! :! :n
-- :doc update an existing availability record
UPDATE availabilities
SET start_time = :start, end_time = :end, provider_id = :provider-id WHERE id = :id

-- :snip join-provider-availability
LEFT JOIN providers ON availabilities.provider_id = providers.id

-- :snip available-between
:sql:conj (start_time BETWEEN :v:start AND :v:end)

-- :snip available-in-state
:sql:conj (providers.state = :v:state)

-- :snip where-available
WHERE :snip*:cond

-- :name get-availabilities :n
-- :doc get availabilities
SELECT * FROM availabilities
--~ (when (:join params) ":snip:join")
--~ (when (:where params) ":snip:where")

-- Example:
-- (get-availabilities {:where
--                      (where-available
--                       {:cond
--                        [(available-between {:start 15234... :end 15234...})
--                         (available-in-state {:state "WA"})]})})

-- :name delete-availability :! :n
-- :doc delete an existing availability record by its id
DELETE FROM availabilities WHERE id = :id


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