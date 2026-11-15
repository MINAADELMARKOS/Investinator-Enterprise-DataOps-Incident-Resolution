"""Authorized read-only tools for evidence-driven, cross-service investigations."""
from dataclasses import dataclass
from typing import Callable

from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from .db import EntityRow, KnowledgeRow, RelationshipRow
from .log_search import search_logs
from .services import blast_radius, latest_metrics


class MetricQuery(BaseModel):
    name: str = Field(min_length=1, max_length=150)


class KnowledgeQuery(BaseModel):
    text: str = Field(min_length=1, max_length=300)


class TopologyQuery(BaseModel):
    entity_id: str = Field(min_length=1, max_length=200)


class LogQuery(BaseModel):
    query: str = Field(min_length=2, max_length=200)
    window_minutes: int = Field(default=60, ge=1, le=240)


class NoArguments(BaseModel):
    pass


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    risk: str
    input_model: type[BaseModel]
    handler: Callable[[Session, str, BaseModel], dict]


def _metrics(db: Session, tenant: str, args: BaseModel) -> dict:
    wanted = MetricQuery.model_validate(args)
    return {"status": "ok", "items": [
        item for item in latest_metrics(db, tenant) if item["name"] == wanted.name
    ][:10]}


def _all_metrics(db: Session, tenant: str, args: BaseModel) -> dict:
    NoArguments.model_validate(args)
    return {"status": "ok", "items": latest_metrics(db, tenant)[:50]}


def _knowledge(db: Session, tenant: str, args: BaseModel) -> dict:
    wanted = KnowledgeQuery.model_validate(args)
    terms = set(wanted.text.lower().split())
    rows = db.scalars(select(KnowledgeRow).where(KnowledgeRow.tenant_id == tenant).limit(200)).all()
    ranked = sorted(
        rows,
        key=lambda row: len(terms.intersection((row.title + " " + row.content).lower().split())),
        reverse=True,
    )
    matches = [row for row in ranked if terms.intersection(
        (row.title + " " + row.content).lower().split()
    )]
    return {"status": "ok", "items": [
        {"id": item.id, "title": item.title, "source": item.source,
         "excerpt": item.content[:1000], "mode": "knowledge"}
        for item in matches[:5]
    ]}


def _topology(db: Session, tenant: str, args: BaseModel) -> dict:
    wanted = TopologyQuery.model_validate(args)
    return {"status": "ok", "downstream": blast_radius(db, tenant, wanted.entity_id)[:60]}


def _upstream(db: Session, tenant: str, args: BaseModel) -> dict:
    wanted = TopologyQuery.model_validate(args)
    start = f"{tenant}:{wanted.entity_id}"
    relationships = db.scalars(select(RelationshipRow).where(RelationshipRow.tenant_id == tenant)).all()
    parents: dict[str, list[str]] = {}
    for relation in relationships:
        parents.setdefault(relation.target_id, []).append(relation.source_id)
    visited = {start}
    frontier = [start]
    for _ in range(4):
        next_nodes = []
        for node in frontier:
            for parent in parents.get(node, []):
                if parent not in visited:
                    visited.add(parent)
                    next_nodes.append(parent)
        frontier = next_nodes
        if not frontier:
            break
    entities = db.scalars(select(EntityRow).where(
        EntityRow.tenant_id == tenant, EntityRow.id.in_(visited)
    )).all()
    return {"status": "ok", "upstream": [
        {"id": item.id.removeprefix(f"{tenant}:"), "kind": item.kind,
         "name": item.name, "criticality": item.criticality}
        for item in entities
    ], "note": "Seeded demo topology unless connected to a live catalog"}


def _logs(db: Session, tenant: str, args: BaseModel) -> dict:
    wanted = LogQuery.model_validate(args)
    return search_logs(tenant, wanted.query, wanted.window_minutes)


TOOLS = {
    "list_current_metrics": ToolSpec(
        "list_current_metrics", "List latest stored service metrics and their simulated/external mode.",
        "READ_ONLY", NoArguments, _all_metrics
    ),
    "get_latest_metric": ToolSpec(
        "get_latest_metric", "Read current stored values for a named metric, including timestamps.",
        "READ_ONLY", MetricQuery, _metrics
    ),
    "search_knowledge": ToolSpec(
        "search_knowledge", "Find matching tenant-scoped operational runbooks and incident knowledge.",
        "READ_ONLY", KnowledgeQuery, _knowledge
    ),
    "get_blast_radius": ToolSpec(
        "get_blast_radius", "Identify downstream entities and potential affected services.",
        "READ_ONLY", TopologyQuery, _topology
    ),
    "get_upstream_dependencies": ToolSpec(
        "get_upstream_dependencies", "Trace upstream dependencies of an entity to find possible causes.",
        "READ_ONLY", TopologyQuery, _upstream
    ),
    "search_logs": ToolSpec(
        "search_logs", "Search recent Elasticsearch/OpenSearch logs for a service or error pattern.",
        "READ_ONLY", LogQuery, _logs
    ),
}


def execute_tool(db: Session, tenant: str, name: str, arguments: dict) -> dict:
    spec = TOOLS.get(name)
    if spec is None or spec.risk != "READ_ONLY":
        raise ValueError("Tool is not allowlisted for investigation")
    return spec.handler(db, tenant, spec.input_model.model_validate(arguments))
