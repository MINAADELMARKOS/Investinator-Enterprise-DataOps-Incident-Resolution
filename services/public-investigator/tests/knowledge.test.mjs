import { test } from "node:test";
import assert from "node:assert/strict";
import { REFERENCES, DEMO_CASES, selectReferences } from "../src/knowledge.mjs";
import { prepareRequest, makeModelInput, extractOutput } from "../src/index.mjs";

test("KRaft context never misclassifies the metadata mode",()=>{
  const refs=selectReferences("Kafka KRaft controller outage and broker lag","kraft");
  assert(refs.some(x=>x.id==="KB-KRAFT-01"));
  assert.equal(DEMO_CASES.kraft.mode,"SIMULATED_KRAFT");
  assert(!DEMO_CASES.kraft.details.includes("ZooKeeper is connected"));
});
test("legacy ZK uses mode-specific reference",()=>{
  assert(selectReferences("ZooKeeper session expiration and legacy Kafka","legacy").some(x=>x.id==="KB-ZK-LEGACY-01"));
});
test("NiFi/HDFS context includes downstream sink reasoning",()=>{
  assert(selectReferences("NiFi PutHDFS backpressure queue","nifi").some(x=>x.id==="KB-NIFI-01"));
});
test("bounds incoming questions, rejects unknown cases",()=>{
  assert.throws(()=>prepareRequest({message:"x".repeat(1201),case_id:"kraft"}),/1200/);
  assert.throws(()=>prepareRequest({message:"show me data",case_id:"invented"}),/valid demo/);
  assert.throws(()=>prepareRequest({message:" "}),/Question/);
});
test("history is bounded and assistant is marked non-live for reference",()=>{
  const input=prepareRequest({case_id:"kraft",message:"How did lag start?",history:Array.from({length:20},(_,i)=>({role:i%2?"assistant":"user",content:"message "+i}))});
  assert.equal(input.history.length,6);
  const msgs=makeModelInput(input);
  assert.equal(msgs.at(-1).content,"How did lag start?");
  assert(msgs[0].content.includes("SIMULATED"));
  assert(msgs[0].content.includes("No shell execution"));
});
test("bounded retrieved knowledge remains a curated reference, not a training dataset",()=>{
  assert(REFERENCES.length>=10);
  assert(selectReferences("Kafka broker lag").length<=5);
  assert(REFERENCES.every(x=>x.id&&x.text.length<700));
});
test("extraction only uses output text messages",()=>{
  assert.equal(extractOutput({output:[{type:"reasoning",summary:[]},{type:"message",content:[{type:"output_text",text:"Inspect KRaft metadata quorum"}]}]}),"Inspect KRaft metadata quorum");
});
