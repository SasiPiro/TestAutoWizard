-- Rules the services check, now also guaranteed by the database: two concurrent requests
-- can both pass a check-then-insert, an index cannot be passed.

-- A plate and a VIN belong to at most one vehicle that is not deleted. Deleted vehicles keep
-- theirs, so the same car can be registered again (e.g. after being sold).
CREATE UNIQUE INDEX uk_vehicles_active_license_plate ON vehicles (license_plate) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_vehicles_active_vin ON vehicles (vin) WHERE deleted_at IS NULL;

-- At most one open (not yet notified) reminder per user and type
CREATE UNIQUE INDEX uk_reminders_open_per_user_and_type ON reminders (user_id, type) WHERE notified = FALSE;
