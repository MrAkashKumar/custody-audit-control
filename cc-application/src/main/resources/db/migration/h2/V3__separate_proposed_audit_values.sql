ALTER TABLE cc_audit_event
  ADD COLUMN proposed_values varchar(24000) DEFAULT '{}' NOT NULL;

-- Existing releases stored the proposed/resulting snapshot in after_values. Retain that
-- evidence as the best available historical proposal while new events distinguish all three.
UPDATE cc_audit_event SET proposed_values = after_values;
