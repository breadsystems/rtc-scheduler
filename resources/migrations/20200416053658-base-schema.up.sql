-- Basic user table, for common data between all user types (doctors, admins, volunteers)
CREATE TABLE users (
  id bigserial PRIMARY KEY,
  email varchar(30),
  pass varchar(300),
  first_name varchar(100),
  last_name varchar(100),
  pronouns varchar(15),
  phone varchar(12),
  state varchar(2),
  last_login timestamp,
  date_created timestamp,
  date_modified timestamp,
  is_admin boolean,
  is_provider boolean,
  authy_id varchar(15),
  preferences json default '{}',
  UNIQUE (email)
);

--;;
-- Invitations for new users.
CREATE TABLE IF NOT EXISTS invitations (
  email varchar(100),
  code varchar(64),
  date_invited timestamp,
  invited_by integer,
  redeemed boolean,
  UNIQUE (email, code),
  FOREIGN KEY (invited_by) REFERENCES users (id) ON DELETE CASCADE
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
  name varchar(100),
  email varchar(100),
  alias varchar(50),
  pronouns varchar(50),
  phone varchar(12),
  ok_to_text boolean,
  date_created timestamp,
  other_needs varchar(200),
  provider_id integer NOT NULL,
  reason varchar(100) NOT NULL,
  provider_notes text,
  transcription text,
  state varchar(2),
  category varchar(100),
  resolution varchar(50),
  FOREIGN KEY (provider_id) REFERENCES users (id) ON DELETE RESTRICT
);

--;;
-- Windows in time when a given doctor is available for appointments
-- (unless they already have an overlapping appointment)

CREATE TABLE IF NOT EXISTS availabilities (
  id bigserial PRIMARY KEY,
  start_time timestamp,
  end_time timestamp,
  provider_id integer NOT NULL,
  FOREIGN KEY (provider_id) REFERENCES users (id) ON DELETE CASCADE
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
