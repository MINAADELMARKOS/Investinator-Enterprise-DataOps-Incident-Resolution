"""Validate audit artifacts without starting services or invoking providers."""
import json
import re
from pathlib import Path

from fastapi.routing import APIRoute
from incident_copilot.api import app
from incident_copilot.db import Base

root = Path(__file__).resolve().parents[3]
docs = root / 'docs/implementation'
snapshot = json.loads((docs / 'baseline/openapi-main.json').read_text())
# Preserve the original audit. Main's GPT merge added only this route description.
current = json.loads((docs / 'prompt09a/openapi-remote-main.json').read_text())
assert current == app.openapi(), 'Synchronized OpenAPI snapshot drift'
original_shape = json.loads(json.dumps(current))
original_shape['paths']['/api/v1/investigator/messages']['post'].pop('description', None)
assert snapshot == original_shape, 'Unexpected change to original API contract'
routes = [r for r in app.routes if isinstance(r, APIRoute)]
matrix = (docs / 'API_PARITY_MATRIX.md').read_text()
for route in routes:
    assert f'`{route.path}`' in matrix, route.path
sql = (docs / 'baseline/schema-postgresql.sql').read_text()
for table in Base.metadata.tables:
    assert f'CREATE TABLE {table} ' in sql, table
for doc in docs.glob('*.md'):
    for target in re.findall(r'\]\(([^)]+)\)', doc.read_text()):
        if '://' not in target and not target.startswith('#'):
            assert (doc.parent / target.split('#')[0]).exists(), (doc, target)
checks = json.loads((docs / 'baseline/checks.json').read_text())
assert any(c['check'] == 'web-audit' and c['exit_code'] != 0 for c in checks)
assert 'release BLOCKED' in (docs / 'STATE.md').read_text()
print(f'PASS: {len(routes)} routes, {len(current["paths"])} paths, '
      f'{len(Base.metadata.tables)} tables, local document links and historical audit findings')
