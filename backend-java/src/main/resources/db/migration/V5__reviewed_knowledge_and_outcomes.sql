CREATE TABLE knowledge_revisions (
 tenant_id varchar(100) NOT NULL, document_id varchar(100) NOT NULL,
 revision integer NOT NULL CHECK(revision>0), content_hash varchar(64) NOT NULL,
 payload jsonb NOT NULL, review_state varchar(30) NOT NULL,
 submitted_by varchar(200) NOT NULL, reviewed_by varchar(200), reviewed_at timestamptz,
 indexed boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant_id,document_id,revision)
);
CREATE TABLE knowledge_outcomes (
 tenant_id varchar(100) NOT NULL, id varchar(64) NOT NULL,
 incident_id varchar(64) NOT NULL, payload jsonb NOT NULL,
 review_state varchar(30) NOT NULL DEFAULT 'PENDING_REVIEW',
 submitted_by varchar(200) NOT NULL, reviewed_by varchar(200), reviewed_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,id)
);
CREATE TABLE knowledge_epochs (tenant_id varchar(100) PRIMARY KEY, version bigint NOT NULL DEFAULT 0);
