CREATE TABLE provider_conversation_state (
 tenant_id varchar(100) NOT NULL, conversation_id varchar(64) NOT NULL,
 ciphertext text NOT NULL, updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,conversation_id)
);
CREATE TABLE citation_evidence (
 tenant_id varchar(100) NOT NULL, incident_id varchar(64) NOT NULL,
 evidence_id varchar(150) NOT NULL, payload jsonb NOT NULL,
 PRIMARY KEY(tenant_id,incident_id,evidence_id)
);

ALTER TABLE ai_usage ADD COLUMN request_count integer NOT NULL DEFAULT 1 CHECK(request_count>0 AND request_count<=10);
