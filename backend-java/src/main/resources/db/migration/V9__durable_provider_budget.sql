-- Private Java schema only. No policy, price or approval is seeded by this migration.
CREATE TABLE provider_budget_accounts (
 tenant_id varchar(100) NOT NULL, model varchar(100) NOT NULL,
 limit_microusd bigint NOT NULL CHECK(limit_microusd>0),
 charged_microusd bigint NOT NULL DEFAULT 0 CHECK(charged_microusd>=0),
 reserved_microusd bigint NOT NULL DEFAULT 0 CHECK(reserved_microusd>=0),
 approval_ref varchar(200) NOT NULL, approved_until timestamptz NOT NULL,
 PRIMARY KEY(tenant_id,model), CHECK((tenant_id='*')=(model='*'))
);
CREATE TABLE provider_budget_prices (
 model varchar(100) PRIMARY KEY,
 input_microusd_per_million bigint NOT NULL CHECK(input_microusd_per_million>0),
 output_microusd_per_million bigint NOT NULL CHECK(output_microusd_per_million>0),
 approval_ref varchar(200) NOT NULL, approved_until timestamptz NOT NULL
);
CREATE TABLE provider_budget_reservations (
 id varchar(64) PRIMARY KEY, tenant_id varchar(100) NOT NULL, model varchar(100) NOT NULL,
 reserved_microusd bigint NOT NULL CHECK(reserved_microusd>0),
 input_price bigint NOT NULL, output_price bigint NOT NULL,
 state varchar(20) NOT NULL CHECK(state IN ('RESERVED','UNKNOWN','SETTLED','OVERDRAWN')),
 actual_microusd bigint, input_tokens bigint, output_tokens bigint,
 approval_ref varchar(200) NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 reconciled_at timestamptz
);
CREATE INDEX budget_reservation_scope ON provider_budget_reservations(tenant_id,model,created_at);
CREATE INDEX remediation_audit_export ON audit_events(tenant_id,(payload->>'plan_id'),created_at,id)
 WHERE event LIKE 'remediation_%';
