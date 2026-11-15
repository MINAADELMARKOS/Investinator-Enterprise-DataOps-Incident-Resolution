"""Integration tests for GPT conversation orchestration and Elasticsearch evidence."""
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from incident_copilot.api import app
from incident_copilot.config import get_settings
from incident_copilot.db import engine
from incident_copilot.log_search import search_logs


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'chat.db'}")
    monkeypatch.setenv("INVESTINATOR_DEMO_MODE", "true")
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.delenv("INVESTINATOR_OPENAI_MODEL", raising=False)
    get_settings.cache_clear()
    engine.cache_clear()
    with TestClient(app) as api:
        yield api
    engine().dispose()
    engine.cache_clear()
    get_settings.cache_clear()


def test_chat_clearly_reports_unconfigured_gpt(client):
    result = client.post("/api/v1/investigator/messages", json={
        "message": "Investigate possible upstream causes of Kafka errors",
        "mode": "investigate",
    })
    assert result.status_code == 200, result.text
    data = result.json()
    assert data["response_mode"] == "deterministic-fallback"
    assert "not configured" in data["answer"]
    assert data["tool_calls"] == []


class FakeFunction:
    type = "function_call"
    name = "list_current_metrics"
    arguments = "{}"
    call_id = "call_01"

    def model_dump(self, **_kwargs):
        return {
            "type": "function_call", "name": self.name,
            "arguments": self.arguments, "call_id": self.call_id,
        }


class FakeResponse:
    def __init__(self, output, text):
        self.output = output
        self.output_text = text
        self.usage = SimpleNamespace(input_tokens=10, output_tokens=20)


class FakeResponses:
    def __init__(self):
        self.requests = []

    def create(self, **kwargs):
        self.requests.append(kwargs)
        if len(self.requests) % 2:
            return FakeResponse([FakeFunction()], "")
        inputs = kwargs["input"]
        tool_output = next(item for item in inputs if isinstance(item, dict)
                           and item.get("type") == "function_call_output")
        assert "items" in tool_output["output"]
        return FakeResponse([], "The available readings are simulated. Inspect the metadata quorum.")


def test_real_conversation_path_uses_model_tools_and_saves_history(client, monkeypatch):
    import openai

    monkeypatch.setenv("OPENAI_API_KEY", "unit-test-key-never-sent")
    monkeypatch.setenv("INVESTINATOR_OPENAI_MODEL", "unit-test-model")
    get_settings.cache_clear()
    responses = FakeResponses()
    monkeypatch.setattr(openai, "OpenAI", lambda **_kwargs: SimpleNamespace(responses=responses))

    first = client.post("/api/v1/investigator/messages", json={
        "mode": "ask", "message": "What does the current Kafka pipeline show?"
    })
    assert first.status_code == 200, first.text
    data = first.json()
    assert data["response_mode"] == "openai"
    assert data["model"] == "unit-test-model"
    assert data["tool_calls"][0]["name"] == "list_current_metrics"
    assert data["input_tokens"] == 20
    assert data["output_tokens"] == 40

    second = client.post("/api/v1/investigator/messages", json={
        "conversation_id": data["conversation_id"],
        "mode": "investigate",
        "message": "What should I investigate next?",
    })
    assert second.status_code == 200, second.text
    assert second.json()["response_mode"] == "openai"
    assert any("What does the current Kafka pipeline show?" in str(item)
               for item in responses.requests[2]["input"])
    history = client.get("/api/v1/investigator/conversations/" + data["conversation_id"])
    assert history.status_code == 200
    assert len(history.json()["messages"]) == 4


def test_elasticsearch_search_keeps_simulated_provenance(monkeypatch):
    monkeypatch.setenv("INVESTINATOR_ELASTICSEARCH_URL", "http://localhost:9200")
    monkeypatch.setenv("INVESTINATOR_ELASTICSEARCH_INDEX", "logs-investinator-demo")
    monkeypatch.setenv("INVESTINATOR_ELASTICSEARCH_TENANT_FIELD", "tenant_id")
    monkeypatch.setenv("INVESTINATOR_DEMO_MODE", "true")
    get_settings.cache_clear()
    captured = {}

    class MockHTTPResponse:
        def raise_for_status(self):
            return None

        def json(self):
            return {"hits": {"hits": [{
                "_index": "logs-investinator-demo",
                "_id": "DEMO-QUORUM-01",
                "_source": {
                    "message": "Kafka KRaft controller heartbeat expired",
                    "source_mode": "simulated",
                    "service": {"name": "kraft-controller"},
                    "@timestamp": "2026-11-15T10:00:00Z",
                },
            }]}}

    def fake_post(url, **kwargs):
        captured["url"] = url
        captured["body"] = kwargs["json"]
        return MockHTTPResponse()

    monkeypatch.setattr("incident_copilot.log_search.httpx.post", fake_post)
    data = search_logs("demo", "KRaft", 60)
    assert data["status"] == "ok"
    assert data["items"][0]["mode"] == "simulated"
    assert data["items"][0]["retrieval_mode"] == "live"
    assert data["items"][0]["id"].startswith("ES-")
    assert {"term": {"tenant_id": "demo"}} in data["items"] or (
        {"term": {"tenant_id": "demo"}} in captured["body"]["query"]["bool"]["filter"]
    )
    assert "/logs-investinator-demo/_search" in captured["url"]
