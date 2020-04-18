-- We are trying to humanize medical care again.
-- To that end, we don't talk about "patients" - we talk about People Seeking Care,
-- People Requiring Care (PRCs) or Careseekers for short.

CREATE TABLE IF NOT EXISTS careseekers (
  id bigserial PRIMARY KEY,
  first_name varchar(100),
  last_name varchar(100),
  email varchar(100),
  alias varchar(50),
  pronouns varchar(15),
  phone varchar(12),
  ok_to_text boolean,
  state varchar(2)
);

--;;
-- Next up, doctors (and potentially other medical professionals)

CREATE TABLE IF NOT EXISTS providers (
  id bigserial PRIMARY KEY,
  first_name varchar(100),
  last_name varchar(100),
  email varchar(100),
  phone varchar(12),
  pronouns varchar(15),
  state varchar(2)
);

--;;
-- Appointments are discrete windows in time with a set start and end date/time.
-- They require some data collected at the end, for basic analytic purposes:
-- these are state, reason, and status.
-- TODO add encryption for notes, transcription?

CREATE TABLE IF NOT EXISTS appointments (
  id bigserial PRIMARY KEY,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  careseeker_id integer NOT NULL,
  provider_id integer,
  notes text,
  transcription text,
  state varchar(2),
  reason varchar(100),
  status varchar(50)
);

--;;
-- Windows in time when a given doctor is available for appointments
-- (unless they already have an overlapping appointment)

CREATE TABLE IF NOT EXISTS availabilities (
  id bigserial PRIMARY KEY,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  provider_id integer NOT NULL
);

--;;
-- A need is something like an access need, such as interpretation

CREATE TABLE IF NOT EXISTS needs (
  id bigserial PRIMARY KEY,
  name varchar(100),
  description text
);

--;;
-- A fulfillment is the counterpart to a need; it is how the need is met.
-- For example, scheduling an interpreter satisfied the need for interpretation

CREATE TABLE IF NOT EXISTS fulfillments (
  id bigserial PRIMARY KEY,
  need_id integer,
  contact_id integer
);

--;;
-- A contact is someone who is not a provider or a careseeker whom we may need to
-- contact for a specific appointment. For example, an interpretation contractor.

CREATE TABLE IF NOT EXISTS contacts (
  id bigserial PRIMARY KEY,
  full_name varchar(100),
  company_name varchar(100),
  title varchar(100),
  phone varchar(12),
  email varchar(100)
);

