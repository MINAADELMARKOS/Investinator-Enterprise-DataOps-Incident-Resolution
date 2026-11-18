CREATE TABLE request_keys (
 tenant_id varchar(100) NOT NULL, route varchar(300) NOT NULL,
 key varchar(128) NOT NULL, request_hash varchar(64) NOT NULL,
 response json NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY (tenant_id, route, key)
);
CREATE TABLE execution_records (
 tenant_id varchar(100) NOT NULL, incident_id varchar(64) NOT NULL,
 action_id varchar(64) NOT NULL, completed_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY (tenant_id,incident_id,action_id)
);
