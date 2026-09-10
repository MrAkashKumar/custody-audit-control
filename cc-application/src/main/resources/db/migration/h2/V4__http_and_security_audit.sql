ALTER TABLE cc_audit_event ADD COLUMN schema_version integer DEFAULT 1 NOT NULL;
ALTER TABLE cc_audit_event ADD COLUMN event_type varchar(255) DEFAULT 'BUSINESS_CHANGE' NOT NULL;
ALTER TABLE cc_audit_event ADD COLUMN endpoint varchar(1000);
ALTER TABLE cc_audit_event ADD COLUMN http_method varchar(16);
ALTER TABLE cc_audit_event ADD COLUMN http_status integer;
ALTER TABLE cc_audit_event ADD COLUMN outcome varchar(64);
ALTER TABLE cc_audit_event ADD COLUMN error_code varchar(255);
ALTER TABLE cc_audit_event ADD COLUMN source_ip varchar(64);
ALTER TABLE cc_audit_event ADD COLUMN user_agent varchar(1000);
ALTER TABLE cc_audit_event ADD COLUMN risk_level varchar(32);
ALTER TABLE cc_audit_event ADD COLUMN duration_ms bigint;
ALTER TABLE cc_audit_event ADD COLUMN metadata varchar(4000) DEFAULT '{}' NOT NULL;

CREATE INDEX cc_idx_audit_event_type_time ON cc_audit_event(event_type, occurred_at, id);
CREATE INDEX cc_idx_audit_http_status_time ON cc_audit_event(http_status, occurred_at, id);
