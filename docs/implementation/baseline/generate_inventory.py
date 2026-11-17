import json,inspect,os,re,sqlite3,tempfile
from pathlib import Path
from sqlalchemy.schema import CreateTable,CreateIndex
from sqlalchemy.dialects import postgresql
from incident_copilot.api import app
from incident_copilot.db import Base
from fastapi.routing import APIRoute
out=Path('docs/implementation/baseline')
out.joinpath('openapi-main.json').write_text(json.dumps(app.openapi(),indent=2,sort_keys=True)+'\n')
ddl=[]
for t in Base.metadata.sorted_tables:
 ddl.append(str(CreateTable(t).compile(dialect=postgresql.dialect()))+';')
 ddl.extend(str(CreateIndex(i).compile(dialect=postgresql.dialect()))+';' for i in sorted(t.indexes,key=lambda i:i.name))
out.joinpath('schema-postgresql.sql').write_text('-- Read-only SQLAlchemy metadata inventory; NOT an executed PostgreSQL migration.\n'+'\n'.join(ddl))
front={str(p):p.read_text() for p in Path('apps/web').rglob('*.tsx') if 'node_modules' not in p.parts and '.next' not in p.parts}
front.update({str(p):p.read_text() for p in [Path('apps/web/lib/api.ts'),Path('apps/web/app/api/v1/[...path]/route.ts')]})
rows=['# API parity matrix','', 'Generated from main FastAPI routes; Java implementation/parity: NOT_IMPLEMENTED for every row.','OpenAPI snapshot preserves request schemas and declared responses. Many responses are generic dict/list; handler source is authoritative, not a complete typed contract. All aliases below remain in scope.','', '| Method | Route | Handler / source | Declared response | Java parity |','|---|---|---|---|---|']
for r in app.routes:
 if isinstance(r,APIRoute):
  source=f'src/incident_copilot/api.py:{inspect.getsourcelines(r.endpoint)[1]}'
  for m in sorted(r.methods): rows.append(f'| {m} | `{r.path}` | `{r.endpoint.__name__}` ({source}) | `{str(r.response_model)}` | NOT_IMPLEMENTED |')
rows+=['','## Frontend consumers (exact call-site evidence)','', 'Dynamic template paths below must retain parameter substitution. `/api/v1/[...path]` forwards browser GET/POST to API_INTERNAL_URL; server pages use lib/api.ts.','']
for p,s in sorted(front.items()):
 matches=[line.strip() for line in s.splitlines() if 'apiGet' in line or 'fetch(' in line]
 if matches: rows+=['### '+p,'','```tsx',*matches,'```','']
rows+=['## Behavioral parity requirements','','Preserve 404 for missing/cross-tenant records, 409 for active replay/action state and unavailable verification, 403 for unapproved or non-ACT-DEMO execution, and 503 for protected endpoints outside demo mode. Preserve server tenant override, response keys, simulation labels, X-Request-ID and security headers. Tests in test_platform.py are behavioral evidence. IncidentResponse/Pydantic definitions are in the snapshot. Dictionary response shapes and conditional errors require differential fixtures before each Java cutover; no endpoint is certified migrated.']
Path('docs/implementation/API_PARITY_MATRIX.md').write_text('\n'.join(rows)+'\n')
out.joinpath('config-presence.json').write_text(json.dumps({n:bool(os.getenv(n)) for n in ['DATABASE_URL','OPENAI_API_KEY','INVESTINATOR_OPENAI_MODEL','KAFKA_BOOTSTRAP_SERVERS','OTEL_EXPORTER_OTLP_ENDPOINT']},indent=2)+'\n')
print('Inventory:',len([r for r in app.routes if isinstance(r,APIRoute)]),'routes;',len(Base.metadata.tables),'tables')
