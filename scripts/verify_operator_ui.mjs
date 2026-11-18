#!/usr/bin/env node
// Owned local lab only. Never logs credentials, cookies, message bodies or error traces.
import { createRequire } from 'node:module';
import { readFileSync, mkdirSync, writeFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
const root = resolve(import.meta.dirname, '..');
const require = createRequire(resolve(root, 'apps/web/package.json'));
const { chromium, expect } = require('@playwright/test');
const state = JSON.parse(readFileSync(resolve(root, '.runtime/lab.json')));
const base = `http://127.0.0.1:${state.ports.LAB_WEB_PORT}`;
const api = `http://127.0.0.1:${state.ports.LAB_API_PORT}/api/v1`;
const out = resolve(root, process.env.UI_REPORT_DIR ?? '.runtime/ui-acceptance');
mkdirSync(out, { recursive: true });
const checks = []; let step = 'launch'; let browser;
function pass(name, provenance = 'LIVE_LOCAL_TRANSPORT / SIMULATED_DATA') { checks.push({ name, result: 'PASS', provenance }); console.log(JSON.stringify({ check: name, result: 'PASS' })); }
async function direct(path, body) { const r = await fetch(api + path, { method: body ? 'POST' : 'GET', headers: { 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined }); if (!r.ok) throw new Error('Native fixture request failed'); return r.json(); }
async function login(role) {
  const context = await browser.newContext(); const page = await context.newPage(); page.setDefaultTimeout(12000);
  await page.goto(base + '/operator/login');
  await page.getByLabel('Demo role', { exact: true }).selectOption(role);
  await page.getByLabel('Role credential').fill(state.secrets[`UI_${role.toUpperCase()}_LOGIN_TOKEN`]);
  await page.getByRole('button', { name: 'Sign in to demo' }).click();
  await page.waitForURL('**/system-health');
  return { context, page };
}
try {
  browser = await chromium.launch({ headless: true, executablePath: process.env.CHROMIUM_PATH || undefined });
  step = 'anonymous boundaries';
  const anonymous = await browser.newContext(); const a = await anonymous.newPage();
  await a.goto(base + '/system-health'); await expect(a).toHaveURL(/operator\/login/);
  if ((await anonymous.request.get(base + '/api/v1/incidents')).status() !== 401) throw new Error('Anonymous read allowed');
  if ((await anonymous.request.post(base + '/api/operator/session', {headers:{Origin:base},data:'null'})).status() !== 422) throw new Error('Malformed session accepted');
  pass(step); await anonymous.close();
  step = 'viewer role and CSRF boundaries';
  const { context, page } = await login('viewer');
  const cookies = await context.cookies(); const cookie = cookies.find(c => c.name === 'investinator_operator');
  if (!cookie?.httpOnly || cookie.sameSite !== 'Strict') throw new Error('Session cookie policy');
  for (const path of ['remediation/plans', 'monitor/pause', 'dependencies/simulations']) {
    const r = await context.request.post(base + '/api/v1/' + path, { headers: { Origin: base }, data: {} });
    if (r.status() !== 403) throw new Error('Viewer capability escaped');
  }
  if ((await context.request.post(base + '/api/v1/investigator/messages', { headers: { Origin: 'https://untrusted.invalid' }, data: { message: 'hello' } })).status() !== 403) throw new Error('CSRF escaped');
  if ((await context.request.get(base + '/api/v1/incidents?tenant=other')).status() !== 422) throw new Error('Tenant override escaped');
  pass(step);
  step = 'native health rendering';
  await expect(page.getByRole('button', { name: 'Refresh diagnostics' })).toBeVisible();
  await page.getByRole('button', { name: 'Refresh diagnostics' }).click();
  await expect(page.getByRole('button', { name: 'Refresh diagnostics' })).toBeEnabled();
  await expect(page.getByText('NOT_CONFIGURED', { exact: true }).first()).toBeVisible();
  await page.screenshot({ path: resolve(out, 'system-health.png'), fullPage: true }); pass(step);
  step = 'unavailable diagnostics preserve uncertainty';
  await page.route('**/api/v1/health/integrations?refresh=true', route => route.fulfill({ status: 503, json: { detail: 'MOCKED outage' } }));
  await page.getByRole('button', { name: 'Refresh diagnostics' }).click();
  await expect(page.getByRole('alert').filter({hasText:'Previous observations'})).toContainText('Previous observations');
  await page.unroute('**/api/v1/health/integrations?refresh=true'); pass(step, 'MOCKED_BROWSER_OUTAGE');
  step = 'general chat and safe message rendering';
  await page.goto(base + '/investigator');
  await page.getByLabel('Message InvestiNator').fill('<img src=x onerror="window.injected=true"> Explain missing data');
  await page.getByRole('button', { name: 'Send →' }).click();
  await expect(page.getByText('Deterministic fallback', { exact: false }).first()).toBeVisible({ timeout: 30000 });
  if (await page.evaluate(() => Boolean(window.injected))) throw new Error('Message executed');
  await page.getByRole('button', { name: 'Reload conversation', exact: true }).click();
  await expect(page.getByText('Deterministic fallback', { exact: false }).first()).toBeVisible();
  const saved = await page.evaluate(() => localStorage.getItem('investinator.conversation.java'));
  if (!saved || saved.includes('<img')) throw new Error('History persistence');
  if ((await context.request.get(base + `/api/v1/investigator/conversations/${saved}/evidence/invented`)).status() !== 404) throw new Error('Invented citation allowed');
  pass(step);
  step = 'browser bundle secret isolation';
  const sources = await page.locator('script[src]').evaluateAll(items => items.map(i => i.src));
  for (const src of sources) { const text = await (await context.request.get(src)).text(); for (const secret of Object.values(state.secrets)) if (typeof secret === 'string' && secret.length >= 24 && text.includes(secret)) throw new Error('Secret in client bundle'); } pass(step);
  step = 'stale conversation';
  await expect(page.getByRole('button', { name: 'Reload conversation', exact: true })).toBeEnabled();
  await page.addInitScript(() => localStorage.setItem('investinator.conversation.java', 'nonexistent-conversation')); 
  await page.reload(); await expect(page.getByRole('alert').filter({hasText:'Conversation unavailable'})).toContainText('Conversation unavailable'); pass(step);
  await context.close();
  step = 'owned replay fixture';
  const run = await direct('/replay/runs', { scenario: 'kafka_disk_pressure' });
  const incident = run.incident_id; let planId;
  step = 'incident chat evidence and history';
  const viewer = await login('viewer'); await viewer.page.goto(base + '/investigator');
  await viewer.page.getByLabel('INCIDENT', { exact: true }).selectOption(incident);
  await viewer.page.getByLabel('Message InvestiNator').fill('Explain this incident with source evidence');
  await viewer.page.getByRole('button', { name: 'Send →' }).click();
  await expect(viewer.page.getByRole('button', { name: 'Inspect evidence', exact: false }).first()).toBeVisible({ timeout: 30000 });
  await viewer.page.getByRole('button', { name: 'Inspect evidence', exact: false }).first().click();
  await expect(viewer.page.getByRole('heading', { name: 'Authorized source excerpt' })).toBeVisible();
  await viewer.page.screenshot({ path: resolve(out, 'incident-chat.png'), fullPage: true });
  await viewer.context.close(); pass(step);
  step = 'actual monitor controls';
  const operator = await login('operator'); await operator.page.goto(base + '/monitoring');
  await operator.page.getByRole('button', { name: 'Refresh monitoring', exact: true }).click();
  await expect(operator.page.getByRole('button', { name: 'Refresh monitoring', exact: true })).toBeEnabled();
  const initialMonitor = await (await operator.context.request.get(base + '/api/v1/monitor/status')).json();
  if (initialMonitor.enabled) {
    await operator.page.getByRole('button', { name: 'Pause demo monitoring', exact: true }).click();
    await expect(operator.page.getByText('Monitoring', { exact: false }).filter({hasText:'PAUSED'}).last()).toBeVisible();
    await operator.page.getByRole('button', { name: 'Resume demo monitoring', exact: true }).click();
    await expect(operator.page.getByText('Monitoring', { exact: false }).filter({hasText:'RUNNING'}).last()).toBeVisible();
    if (initialMonitor.paused) await operator.page.getByRole('button', { name: 'Pause demo monitoring', exact: true }).click();
  }
  await operator.page.screenshot({ path: resolve(out, 'monitoring.png'), fullPage: true }); pass(step);
  const graphMarker = resolve(root, '.runtime/causal-kafka-kraft.json');
  if (existsSync(graphMarker)) {
    step = 'bound incident graph and uncertainty';
    const causal = JSON.parse(readFileSync(graphMarker));
    await operator.page.goto(base + '/nervemap');
    await operator.page.getByLabel('Monitored incident graph').selectOption(causal.incident_id);
    await expect(operator.page.getByText('Hypothesis 1: network', { exact: false })).toBeVisible();
    await expect(operator.page.getByText('None — correlation is not causal confirmation', { exact: false })).toBeVisible();
    await operator.page.getByText('Time-aligned observations', { exact: true }).click();
    await expect(operator.page.getByRole('cell', { name: 'network · NETWORK_UNSTABLE', exact: true })).toHaveCount(2);
    await operator.page.screenshot({ path: resolve(out, 'bound-graph.png'), fullPage: true }); pass(step);
  }
  await operator.context.close();
  step = 'governed workflow';
  async function planPage(role) { const r = await login(role); await r.page.goto(base + '/incidents/' + incident); await expect(r.page.getByRole('button', { name: 'Refresh durable status' })).toBeVisible(); if (planId) { await r.page.getByLabel('Stored plan').selectOption(planId); await expect(r.page.getByText('Immutable action review', { exact: false })).toBeVisible(); } return r; }
  async function reviewed(p) { await p.getByRole('checkbox', { name: 'I reviewed this exact target', exact: false }).check(); }
  async function cycle(rollback) {
    let r = await planPage('planner');
    await r.page.getByRole('button', { name: rollback ? 'Dry run named rollback' : 'Dry run recovery', exact: true }).click();
    await expect(r.page.getByText('Dry run only.', { exact: false })).toBeVisible();
    await r.page.getByRole('button', { name: rollback ? 'Propose separate rollback' : 'Create governed proposal', exact: true }).click();
    await expect(r.page.getByText('Proposal persisted;', { exact: false })).toBeVisible();
    planId = await r.page.getByLabel('Stored plan').inputValue();
    await r.page.screenshot({ path: resolve(out, rollback ? 'rollback-review.png' : 'immutable-plan-review.png'), fullPage: true }); await r.context.close();
    r = await planPage('approver'); await reviewed(r.page); await r.page.getByRole('button', { name: 'Approve exact plan', exact: true }).click();
    await expect(r.page.getByText('Immutable action review · AUTHORIZED', { exact: true })).toBeVisible(); await r.context.close();
    r = await planPage('executor'); await reviewed(r.page); await r.page.getByRole('button', { name: 'Execute approved simulation', exact: true }).click();
    await expect(r.page.getByText('Executor acknowledged.', { exact: false })).toBeVisible(); await r.context.close();
    r = await planPage('verifier'); await reviewed(r.page); await r.page.getByRole('button', { name: 'Verify independent post-state', exact: true }).click();
    await expect(r.page.getByText('Verification did not pass.', { exact: false })).toBeVisible();
    await r.page.getByRole('button', { name: 'Sample current simulator state', exact: true }).click();
    await expect(r.page.getByText('Server policy recorded the operation.', { exact: false })).toBeVisible();
    await reviewed(r.page); await r.page.getByRole('button', { name: 'Verify independent post-state', exact: true }).click();
    await expect(r.page.getByText('Independent SIMULATED post-state verified.', { exact: true })).toBeVisible(); await r.context.close();
    pass(rollback ? 'separate named rollback with independent sample' : 'distinct demo capabilities and independent verification');
  }
  await cycle(false); await cycle(true);
  if ((await direct('/incidents/' + incident)).status !== 'closed_unresolved') throw new Error('Rollback lifecycle');
  step = 'tampered cookie'; const tampered = await browser.newContext();
  await tampered.addCookies([{ ...cookie, value: cookie.value.slice(0, -3) + 'bad' }]);
  if ((await tampered.request.get(base + '/api/v1/incidents')).status() !== 401) throw new Error('Tampered session allowed'); await tampered.close(); pass(step);
  if (process.env.PYTHON_WEB_URL) {
    step = 'actual default Python browser compatibility'; const c = await browser.newContext(); const p = await c.newPage();
    await p.goto(process.env.PYTHON_WEB_URL + '/'); await expect(p.getByText('SIMULATED DEMO', { exact: false }).first()).toBeVisible();
    const response = await c.request.get(process.env.PYTHON_WEB_URL + '/api/operator/session');
    if ((await response.json()).backend !== 'python') throw new Error('Python default missing');
    await p.goto(process.env.PYTHON_WEB_URL + '/investigator'); await p.getByLabel('Message InvestiNator').fill('What evidence is missing?'); await p.getByRole('button', { name: 'Send →' }).click();
    await expect(p.getByText('Deterministic fallback', { exact: false }).first()).toBeVisible({ timeout: 30000 });
    await p.screenshot({ path: resolve(out, 'python-default.png'), fullPage: true }); await c.close(); pass(step);
  }
  writeFileSync(resolve(out, 'results.json'), JSON.stringify({ status: 'PASS', checks, paid_provider_calls: 0, production_actions: 0, enterprise_identity: 'NOT_CERTIFIED', python_browser: process.env.PYTHON_WEB_URL ? 'PASS' : 'NOT_TESTED' }, null, 2));
  console.log(JSON.stringify({ status: 'PASS', checks: checks.length, report: out }));
} catch (error) {
  writeFileSync(resolve(out, 'results.json'), JSON.stringify({ status: 'FAIL', step, error_type: error.name, checks }, null, 2));
  console.error(JSON.stringify({ status: 'FAIL', step, error_type: error.name, reason: step.includes('boundaries') ? 'Boundary check failed' : String(error.message).startsWith('expect(') ? String(error.message).split('\n').slice(0,4).join(' ') : 'Browser operation failed' })); process.exitCode = 1;
} finally { await browser?.close(); }
