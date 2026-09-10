ALTER TABLE cc_security_change ADD COLUMN baseline varchar(16000) DEFAULT '{}';
ALTER TABLE cc_security_change ALTER COLUMN reason varchar(1000);
ALTER TABLE cc_security_change ALTER COLUMN decision_reason varchar(1000);
ALTER TABLE cc_report_job ADD COLUMN claimed_at timestamp(6) with time zone;
