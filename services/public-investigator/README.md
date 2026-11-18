# InvestiNator Public Investigator — secure GPT gateway

**Status:** Implementation and no-cost tests can be verified on GitHub. **Live GPT is not enabled by simply merging this code.** The public GitHub Pages website is a static Vite bundle and cannot store OpenAI credentials or execute a Java backend. Deploy this Worker separately after connecting your own Cloudflare account and OpenAI API project.

## Purpose and knowledge scope

Public chat is **not model fine-tuning**. It uses real GPT Responses API calls with small, versioned, public, author-curated RAG reference excerpts from `src/knowledge.mjs`.

The domain focus is **upstream root cause investigation** across Kafka KRaft vs legacy ZooKeeper, Spark/YARN, HDFS HA, Hive/HMS/HS2, Kudu, NiFi backpressure, Airflow/Oozie, metrics, network behavior, dependency topology and dataset/business freshness.

Each user question is grounded in selected runbook snippets, exact `KB-...` references and one explicitly **SIMULATED** example incident. That example is not telemetry from a production cluster. The public gateway has no Elasticsearch, Kafka, HDFS or other system credentials, no shell commands and no remediation tools.

For actual enterprise evidence, the private Spring Boot / Java Investigator is the separate opt-in application with capability-based connectors, tenant scopes, reviewed knowledge and function calling. Do not connect those private endpoints to an anonymous public site.

## Security controls

- `OPENAI_API_KEY` lives **only** in a Cloudflare secret, never in GitHub Pages or a browser variable.
- `TURNSTILE_SECRET` and a verified hostname are required on **every** paid request. Use a Turnstile site widget registered for `minaadelmarkos.github.io`.
- SQLite Durable Object **global and per-client daily/minute budgets** are checked atomically before paying for a request. Current caps in source: 50 total turns/day, 8 turns per anonymized IP/day, 3 turns per anonymized IP/minute. `QUOTA_SALT` is a secret hashed with the client IP; raw IP is not logged or stored in the quota ledger.
- Exactly one approved browser `Origin` is allowed, with Turnstile hostname verification. CORS and Origin are not identity/authentication.
- Requests are capped at 12 KB, question 1200 characters, last 6 history messages, model output 950 tokens, provider timeout 23 seconds. No arbitrary URL fetch or function calls.
- OpenAI `store:false`; the public gateway itself doesn't retain chat text. Submitted text is still sent to a third-party provider. Do not enter personal information, customer data or proprietary logs.
- There is no live monitoring, administrative API, production executor, incident approval or real cluster diagnosis. All fault examples are synthetic.
- Set **OpenAI project budget/rate limits** independently from Worker quotas. Worker limits count *attempted* requests and are safeguards, not guaranteed hard financial caps against every possible billing scenario.
- **Fail closed:** without all environment variables and Durable Object binding, `/health` says not ready and `/chat` returns HTTP 503. GitHub Pages falls back to a clearly labeled deterministic guided demo.

## Cloudflare setup (explicit account action required)

Cloudflare Workers and Durable Objects need an eligible Cloudflare account. A Turnstile site key/secret and OpenAI API project with approved spending are also required.

```bash
cd services/public-investigator
npm install
npm run check
npm test
npx wrangler login
```

Edit `wrangler.toml` to set the **enabled OpenAI model** in `[vars]`:

```toml
OPENAI_MODEL = "YOUR_ENABLED_MODEL_ID"
```

Then add the **secrets interactively**, not to the repository:

```bash
npx wrangler secret put OPENAI_API_KEY
npx wrangler secret put TURNSTILE_SECRET
npx wrangler secret put TURNSTILE_SITE_KEY
npx wrangler secret put QUOTA_SALT
npx wrangler deploy
```

The last command returns a URL such as `https://investinator-public-investigator.<YOUR_CLOUDFLARE_SUBDOMAIN>.workers.dev`. Do not assume this address exists until deployment succeeds.

Confirm the Worker allows the GitHub Pages origin and its `GET /health` reply returns `ready: true` **when requested from that origin**.

## Connect GitHub Pages

On GitHub:

1. Repository **Settings → Secrets and variables → Actions → Variables**.
2. Create a **repository variable** named `INVESTINATOR_PUBLIC_CHAT_API`. Its value is the actual HTTPS Worker URL with no trailing slash.
3. Run the GitHub Pages workflow again from `main` (or push a docs commit) so Vite embeds the public gateway URL in the static JS bundle.
4. Visit `https://minaadelmarkos.github.io/Investinator-Enterprise-DataOps-Incident-Resolution/#/investigator`.

The Pages workflow maps that public **URL only** to `VITE_INVESTINATOR_PUBLIC_CHAT_API`. Never put `OPENAI_API_KEY`, `TURNSTILE_SECRET` or other private credentials in a `VITE_` variable or GitHub Pages secrets copied to build output.

With an installed, reachable Worker and validated Turnstile site, the badge changes to **LIVE GPT GATEWAY READY** and the user can complete a challenge and send a real question. Until then, it says **OFFLINE GUIDED PREVIEW** and produces no paid model calls.

### Why no public Java endpoint here?

The Java backend currently uses demo-specific capabilities and a private Flyway schema, not an independently certified enterprise ingress. Exposing it publicly would bypass the deliberate production blockers P14-01/06/07. A separately limited public educational chatbot is the safe initial live interaction.

## Validation checklist

```bash
node --check src/index.mjs
node --check src/knowledge.mjs
npm test
```

The CI job `public-investigator` runs no-cost tests, while `showcase` builds the browser UI. **Neither tests an actual paid GPT response.** Before claiming live functionality, perform one authorized request and retain a redacted success record with timestamp, model, source mode, server-side request count, budget state, and zero infrastructure writes.

Test Turnstile failure, wrong origin, missing secrets, quota exhaustion, provider outage and inappropriate production-action requests as negative acceptance checks. Log request statuses and durations only, not full questions or model responses.

## Product roadmap

Promote additional knowledge only through licensed, reviewed runbooks and postmortems with version/mode metadata. Do not ingest private enterprise logs into the public gateway. Evaluate question relevance, evidence/citation validity, abstention and confusion between KRaft/legacy ZooKeeper on a human-reviewed holdout set. If deeper multi-system telemetry is needed, use the secured Java Investigator instead of adding public tools.
