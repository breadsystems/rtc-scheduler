-- Basic user table, for common data between all user types (doctors, admins, volunteers)
CREATE TABLE IF NOT EXISTS users (
  id bigserial PRIMARY KEY,
  email varchar(30),
  pass varchar(300),
  first_name varchar(100),
  last_name varchar(100),
  pronouns varchar(15),
  phone varchar(12),
  last_login timestamp,
  date_created timestamp,
  date_modified timestamp,
  is_admin boolean,
  authy_id varchar(15),
  preferences json default '{}',
  UNIQUE (email)
);

--;;
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
  state varchar(2),
  date_created timestamp,
  date_modified timestamp,
  UNIQUE (alias)
);

--;;
-- Next up, doctors (and potentially other medical professionals)

CREATE TABLE IF NOT EXISTS providers (
  id int PRIMARY KEY,
  state varchar(2),
  date_created timestamp,
  date_modified timestamp,
  FOREIGN KEY (id) REFERENCES users (id) ON DELETE RESTRICT
);

--;;
-- Appointments are discrete windows in time with a set start and end date/time.
-- They require some data collected at the end, for basic analytic purposes:
-- these are state, reason, and status.
-- TODO add encryption for notes, transcription?

CREATE TABLE IF NOT EXISTS appointments (
  id bigserial PRIMARY KEY,
  start_time timestamp NOT NULL,
  end_time timestamp NOT NULL,
  careseeker_id integer NOT NULL,
  provider_id integer NOT NULL,
  reason varchar(100) NOT NULL,
  provider_notes text,
  transcription text,
  state varchar(2),
  category varchar(100),
  resolution varchar(50),
  FOREIGN KEY (careseeker_id) REFERENCES careseekers (id) ON DELETE RESTRICT,
  FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE RESTRICT
);

--;;
-- Windows in time when a given doctor is available for appointments
-- (unless they already have an overlapping appointment)

CREATE TABLE IF NOT EXISTS availabilities (
  id bigserial PRIMARY KEY,
  start_time timestamp,
  end_time timestamp,
  provider_id integer NOT NULL,
  FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE CASCADE
);

--;;
-- A need is something like an access need, such as interpretation

CREATE TABLE IF NOT EXISTS needs (
  id bigserial PRIMARY KEY,
  name varchar(100),
  description text,
  UNIQUE (name)
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

--;;
-- Associate appointment with a specific need

CREATE TABLE IF NOT EXISTS appointment_needs (
  appointment_id int NOT NULL,
  need_id int NOT NULL,
  info text,
  contact_id int,
  PRIMARY KEY (need_id, appointment_id),
  FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE RESTRICT,
  FOREIGN KEY (need_id) REFERENCES needs (id) ON DELETE RESTRICT,
  FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE RESTRICT
);
