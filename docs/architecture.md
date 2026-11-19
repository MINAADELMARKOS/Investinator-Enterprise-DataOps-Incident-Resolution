# InvestiNator architecture — current entry

The versioned [Architecture v2 handover](handover/ARCHITECTURE.md) is the current
context/container/component/deployment/sequence/data-flow view. It maps actual Java
modules, conditional APIs and private migrations, coexisting with preserved Python.
The [status matrix](handover/STATUS_MATRIX.md) separates built, tested, simulated,
mocked, unconfigured and planned capabilities, with actual CI/source provenance.

Next defaults to Python; the owned eight-service lab explicitly selects Java.
Java is a modular monolith, not separately deployed agent services. PostgreSQL/
Flyway schema and Python/Alembic ownership stay separate. Actual local transports
carry synthetic faults; optional real GPT is independent of telemetry provenance.
The model is a read-only investigator. Simulator execution requires separate policy,
exact approval, effect receipt and independently sampled verification. Enterprise
identity, certified private connectors, external change control and release gates
remain BLOCKED. No automatic production route change or executor is installed.

For historical requirements/business hypotheses see
[implementation architecture](implementation/ARCHITECTURE.md); original v2 target
assumptions are retained in [roadmap](roadmap/JAVA_AI_ARCHITECTURE_V2.md) and do not
supersede current evidence. Numeric production benefits remain unmeasured.
