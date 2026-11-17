-- Read-only SQLAlchemy metadata inventory; NOT an executed PostgreSQL migration.

CREATE TABLE ai_usage (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	incident_id VARCHAR(64), 
	task VARCHAR(60) NOT NULL, 
	model VARCHAR(100) NOT NULL, 
	latency_ms INTEGER NOT NULL, 
	input_tokens INTEGER NOT NULL, 
	output_tokens INTEGER NOT NULL, 
	status VARCHAR(30) NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_ai_usage_tenant_id ON ai_usage (tenant_id);

CREATE TABLE approvals (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	incident_id VARCHAR(64) NOT NULL, 
	action_id VARCHAR(64) NOT NULL, 
	decision VARCHAR(20) NOT NULL, 
	actor VARCHAR(200) NOT NULL, 
	reason VARCHAR(2000) NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE UNIQUE INDEX ix_approval_incident_action ON approvals (tenant_id, incident_id, action_id);
CREATE INDEX ix_approvals_incident_id ON approvals (incident_id);
CREATE INDEX ix_approvals_tenant_id ON approvals (tenant_id);

CREATE TABLE audit_events (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	incident_id VARCHAR(64), 
	actor VARCHAR(200) NOT NULL, 
	event VARCHAR(100) NOT NULL, 
	payload JSON NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_audit_events_incident_id ON audit_events (incident_id);
CREATE INDEX ix_audit_events_tenant_id ON audit_events (tenant_id);

CREATE TABLE conversations (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	incident_id VARCHAR(64), 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_conversations_tenant_id ON conversations (tenant_id);

CREATE TABLE entities (
	id VARCHAR(200) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	kind VARCHAR(60) NOT NULL, 
	name VARCHAR(200) NOT NULL, 
	criticality INTEGER NOT NULL, 
	payload JSON NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_entities_tenant_id ON entities (tenant_id);

CREATE TABLE evaluations (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	replay_id VARCHAR(64), 
	score FLOAT NOT NULL, 
	payload JSON NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_evaluations_tenant_id ON evaluations (tenant_id);

CREATE TABLE incidents (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	environment VARCHAR(100) NOT NULL, 
	status VARCHAR(40) NOT NULL, 
	severity VARCHAR(10) NOT NULL, 
	technology VARCHAR(40) NOT NULL, 
	payload JSON NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	updated_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_incidents_severity ON incidents (severity);
CREATE INDEX ix_incidents_status ON incidents (status);
CREATE INDEX ix_incidents_tenant_id ON incidents (tenant_id);
CREATE INDEX ix_incidents_tenant_status_created ON incidents (tenant_id, status, created_at);

CREATE TABLE knowledge_documents (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	title VARCHAR(300) NOT NULL, 
	technology VARCHAR(50) NOT NULL, 
	content VARCHAR(20000) NOT NULL, 
	source VARCHAR(500) NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_knowledge_documents_tenant_id ON knowledge_documents (tenant_id);

CREATE TABLE relationships (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	source_id VARCHAR(200) NOT NULL, 
	target_id VARCHAR(200) NOT NULL, 
	kind VARCHAR(60) NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_relationships_source_id ON relationships (source_id);
CREATE INDEX ix_relationships_target_id ON relationships (target_id);
CREATE INDEX ix_relationships_tenant_id ON relationships (tenant_id);

CREATE TABLE replay_runs (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	scenario VARCHAR(100) NOT NULL, 
	incident_id VARCHAR(64), 
	status VARCHAR(40) NOT NULL, 
	payload JSON NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_replay_runs_tenant_id ON replay_runs (tenant_id);

CREATE TABLE telemetry (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	entity_id VARCHAR(200) NOT NULL, 
	signal_type VARCHAR(30) NOT NULL, 
	name VARCHAR(150) NOT NULL, 
	numeric_value FLOAT, 
	observed_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	payload JSON NOT NULL, 
	PRIMARY KEY (id)
)

;
CREATE INDEX ix_telemetry_entity_id ON telemetry (entity_id);
CREATE INDEX ix_telemetry_name ON telemetry (name);
CREATE INDEX ix_telemetry_observed_at ON telemetry (observed_at);
CREATE INDEX ix_telemetry_signal_type ON telemetry (signal_type);
CREATE INDEX ix_telemetry_tenant_id ON telemetry (tenant_id);
CREATE INDEX ix_telemetry_tenant_name_time ON telemetry (tenant_id, name, observed_at);

CREATE TABLE alerts (
	id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	incident_id VARCHAR(64), 
	rule_id VARCHAR(100) NOT NULL, 
	entity_id VARCHAR(200) NOT NULL, 
	severity VARCHAR(10) NOT NULL, 
	observed_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	payload JSON NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(incident_id) REFERENCES incidents (id)
)

;
CREATE INDEX ix_alerts_entity_id ON alerts (entity_id);
CREATE INDEX ix_alerts_incident_id ON alerts (incident_id);
CREATE INDEX ix_alerts_tenant_id ON alerts (tenant_id);

CREATE TABLE messages (
	id VARCHAR(64) NOT NULL, 
	conversation_id VARCHAR(64) NOT NULL, 
	tenant_id VARCHAR(100) NOT NULL, 
	role VARCHAR(20) NOT NULL, 
	content VARCHAR(10000) NOT NULL, 
	payload JSON NOT NULL, 
	created_at TIMESTAMP WITH TIME ZONE NOT NULL, 
	PRIMARY KEY (id), 
	FOREIGN KEY(conversation_id) REFERENCES conversations (id)
)

;
CREATE INDEX ix_messages_conversation_id ON messages (conversation_id);
CREATE INDEX ix_messages_tenant_id ON messages (tenant_id);