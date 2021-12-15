-- Track appointment reminders in the appointments table.
ALTER TABLE appointments
ADD COLUMN reminded_careseeker BOOLEAN DEFAULT false,
ADD COLUMN reminded_provider BOOLEAN DEFAULT false;
