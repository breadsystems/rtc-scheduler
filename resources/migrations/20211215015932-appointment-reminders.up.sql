-- Track appointment reminders in the appointments table.
ALTER TABLE appointments
ADD COLUMN reminded_careseeker BOOLEAN,
ADD COLUMN reminded_provider BOOLEAN;
