CREATE TABLE dependency_graphs (
 tenant_id varchar(100) NOT NULL, graph_id varchar(100) NOT NULL,
 revision integer NOT NULL CHECK (revision>0), payload jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,graph_id,revision)
);
CREATE TABLE correlation_observations (
 tenant_id varchar(100) NOT NULL, incident_id varchar(64) NOT NULL,
 evidence_id varchar(150) NOT NULL, payload jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,incident_id,evidence_id)
);
CREATE TABLE incident_graph_bindings (
 tenant_id varchar(100) NOT NULL, incident_id varchar(64) NOT NULL,
 graph_id varchar(100) NOT NULL, revision integer NOT NULL,
 PRIMARY KEY(tenant_id,incident_id),
 FOREIGN KEY(tenant_id,graph_id,revision) REFERENCES dependency_graphs(tenant_id,graph_id,revision)
);
