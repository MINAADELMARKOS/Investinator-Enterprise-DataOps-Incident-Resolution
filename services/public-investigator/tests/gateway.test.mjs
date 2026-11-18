import { test } from "node:test";
import assert from "node:assert/strict";
import worker, {prepareRequest,makeModelInput} from "../src/index.mjs";

const origin="https://minaadelmarkos.github.io";
test("health endpoint is fail-closed without server-side config",async()=>{
  const r=await worker.fetch(new Request("https://worker.example/health",{headers:{Origin:origin}}),{});
  assert.equal(r.status,200);
  const body=await r.json();
  assert.equal(body.ready,false);
  assert.equal(body.site_key,null);
  assert.equal(r.headers.get("Access-Control-Allow-Origin"),origin);
});
test("cross-origin health is denied",async()=>{
  const r=await worker.fetch(new Request("https://worker.example/health",{headers:{Origin:"https://attacker.example"}}),{});
  assert.equal(r.status,403);
  assert.equal(r.headers.get("Access-Control-Allow-Origin"),null);
});
test("unconfigured provider blocks calls even with valid body",async()=>{
  const r=await worker.fetch(new Request("https://worker.example/chat",{method:"POST",headers:{Origin:origin,"Content-Type":"application/json"},body:JSON.stringify({case_id:"kraft",message:"Investigate the controller"})}),{});
  assert.equal(r.status,503);
  const data=await r.json();
  assert.equal(data.status,"NOT_CONFIGURED");
});
test("no direct infrastructure or user-selected tool is emitted",()=>{
  const prepared=prepareRequest({case_id:"legacy",message:"Restart production brokers and delete topic now"});
  const input=makeModelInput(prepared);
  assert(!input.some(x=>x.type==="function_call"));
  assert(input[0].content.includes("No shell execution"));
  assert(input[0].content.includes("production changes or approvals"));
  assert(input[1].content.includes("SIMULATED_LEGACY_ZOOKEEPER"));
});

const fixtureConfig={OPENAI_API_KEY:"fixture-only-not-a-provider-key",OPENAI_MODEL:"fixture-model",TURNSTILE_SECRET:"fixture",TURNSTILE_SITE_KEY:"fixture",QUOTA_SALT:"fixture",PUBLIC_QUOTA:{},OPENAI_ENABLED:"true",OPENAI_PAID_OPT_IN:"true",OPENAI_COST_APPROVED:"true",MODEL_TRANSFER_APPROVED:"true",OPENAI_APPROVAL_REF:"SIMULATED-APPROVAL"};
test("key and model without separate cost and transfer opt-in cannot bill",async()=>{
  const env={...fixtureConfig,OPENAI_COST_APPROVED:"false"};
  const r=await worker.fetch(new Request("https://worker.example/chat",{method:"POST",headers:{Origin:origin,"Content-Type":"application/json"},body:JSON.stringify({case_id:"kraft",message:"Investigate controller"})}),env);
  assert.equal(r.status,503);assert.equal((await r.json()).status,"NOT_CONFIGURED");
});
test("chunked body exceeding actual byte bound is cancelled before verification or provider",async()=>{
  let cancelled=false;
  const stream=new ReadableStream({pull(controller){controller.enqueue(new Uint8Array(6001));},cancel(){cancelled=true;}});
  const request=new Request("https://worker.example/chat",{method:"POST",headers:{Origin:origin,"Content-Type":"application/json"},body:stream,duplex:"half"});
  const r=await worker.fetch(request,fixtureConfig);assert.equal(r.status,413);assert.equal(cancelled,true);
});
test("invalid UTF-8 cannot silently enter a public prompt",async()=>{
  const r=await worker.fetch(new Request("https://worker.example/chat",{method:"POST",headers:{Origin:origin,"Content-Type":"application/json"},body:new Uint8Array([0xc3,0x28])}),fixtureConfig);
  assert.equal(r.status,422);
});
