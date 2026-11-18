import { useEffect, useRef, useState, type FormEvent } from "react";
import { investigationCases } from "./java-preview-data";
import { Bot, BookOpen, ExternalLink, RotateCcw, ShieldCheck, Wifi, WifiOff } from "lucide-react";

type CaseId = "kraft" | "legacy" | "nifi";
type Turn = { role:"user"|"assistant"; content:string; mode:"LIVE_GPT_PUBLIC_RAG"|"OFFLINE_GUIDED_DEMO"; refs?:{id:string;title:string;service:string;url?:string|null}[] };
type PublicChatConfig = {ready:boolean;mode:string;site_key:string|null;source:string};
type Turnstile = {
  render:(element:HTMLElement,opts:{sitekey:string;callback:(token:string)=>void;"expired-callback":()=>void;"error-callback":()=>void})=>string;
  reset:(id:string)=>void;
  remove:(id:string)=>void;
};
declare global { interface Window {turnstile?:Turnstile} }
const apiEndpoint = String(import.meta.env.VITE_INVESTINATOR_PUBLIC_CHAT_API||"").replace(/\/$/,"");
const isSecureEndpoint = (()=>{try {return new URL(apiEndpoint).protocol==="https:";}catch{return false;}})();
const scenarioOptions: {key:CaseId;title:string}[] = [
  {key:"kraft",title:"Kafka 4.x: Network → KRaft → Consumer lag"},
  {key:"legacy",title:"Legacy Kafka: ZooKeeper session instability"},
  {key:"nifi",title:"NiFi: HDFS sink failure → backpressure"},
];
const suggestedQuestions=[
  "Why does Kafka show errors when the originating failure is elsewhere?",
  "Which logs and metrics help distinguish the root cause from symptoms?",
  "What downstream data products are affected?",
  "Which checks are safe to run before proposing remediation?",
];
function guidedAnswer(question:string,caseId:CaseId) {
  const scenario=investigationCases.find(s=>s.key===caseId)||investigationCases[0];
  const support=scenario.events.map(e=>e.id).join(", ");
  const focus=/fix|recover|remediat|restart/i.test(question)
    ? "Safe next step: investigate with read-only service and network checks. Real remediation requires authenticated approval and independent post-state verification."
    : /impact|downstream|business/i.test(question)
      ? "Potential downstream impact: "+scenario.impact+". These are example dependencies; no revenue impact is measured."
      : /log|metric|check|evidence/i.test(question)
        ? "Read-only checks: "+scenario.checks.join("; ")+"."
        : "The earlier upstream abnormality might explain later errors, but event order alone does not prove causality.";
  return `OFFLINE GUIDED EXAMPLE — not a GPT response and not live telemetry.

Selected example: ${scenario.title}
Most plausible originating hypothesis: ${scenario.origin}.
Supporting synthetic IDs: ${support}.

${focus}

Competing explanations:
${scenario.hypotheses.slice(1).map(h=>"- "+h.name+" — "+h.contradiction).join("\n")}

To get generated answers to arbitrary questions, the public GPT gateway must be configured, protected with Turnstile and connected to this UI.`;
}
export default function LiveInvestigator() {
  const [selected,setSelected]=useState<CaseId>("kraft");
  const [configuration,setConfiguration]=useState<PublicChatConfig|null>(null);
  const [status,setStatus]=useState<"CHECKING"|"LIVE_READY"|"NOT_CONFIGURED"|"UNAVAILABLE">("CHECKING");
  const [input,setInput]=useState("");
  const [turns,setTurns]=useState<Turn[]>([]);
  const [pending,setPending]=useState(false);
  const [error,setError]=useState("");
  const [captcha,setCaptcha]=useState("");
  const [challengeError,setChallengeError]=useState("");
  const widgetRef=useRef<HTMLDivElement>(null),idRef=useRef<string|null>(null),lastRef=useRef<HTMLDivElement>(null);
  const live=status==="LIVE_READY";
  const scenario=investigationCases.find(s=>s.key===selected)||investigationCases[0];
  useEffect(()=>{
    let active=true;
    if(!isSecureEndpoint){setStatus("NOT_CONFIGURED");return;}
    const controller=new AbortController();
    const timer=setTimeout(()=>controller.abort(),8000);
    fetch(apiEndpoint+"/health",{headers:{Accept:"application/json"},signal:controller.signal,cache:"no-store"})
      .then(async response=>{if(!response.ok)throw new Error("Gateway unavailable");return await response.json() as PublicChatConfig;})
      .then(payload=>{if(!active)return;setConfiguration(payload);setStatus(payload.ready&&payload.site_key?"LIVE_READY":"NOT_CONFIGURED");})
      .catch(()=>{if(active)setStatus("UNAVAILABLE");})
      .finally(()=>clearTimeout(timer));
    return ()=>{active=false;controller.abort();clearTimeout(timer);};
  },[]);
  useEffect(()=>{
    if(!live||!configuration?.site_key)return;
    let active=true,timeout:number|undefined;
    const render=()=>{
      if(!active||!widgetRef.current||!window.turnstile||idRef.current)return;
      idRef.current=window.turnstile.render(widgetRef.current,{
        sitekey:configuration.site_key!,
        callback:token=>{if(active){setCaptcha(token);setChallengeError("");}},
        "expired-callback":()=>{if(active)setCaptcha("");},
        "error-callback":()=>{if(active){setCaptcha("");setChallengeError("Verification unavailable. Retry the challenge.");}},
      });
    };
    if(window.turnstile){render();}
    else {
      const script=document.createElement("script");
      script.src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
      script.async=true;script.defer=true;
      script.onload=render;
      script.onerror=()=>{if(active)setChallengeError("Verification script unavailable");};
      document.head.appendChild(script);
      timeout=window.setInterval(render,350);
    }
    return ()=>{active=false;if(timeout)clearInterval(timeout);if(idRef.current&&window.turnstile){window.turnstile.remove(idRef.current);idRef.current=null;}};
  },[live,configuration?.site_key]);
  useEffect(()=>lastRef.current?.scrollIntoView({behavior:"smooth",block:"nearest"}),[turns,pending]);
  function reset(){setTurns([]);setInput("");setError("");}
  function changeScenario(value:CaseId){setSelected(value);reset();}
  async function send(event:FormEvent){
    event.preventDefault();
    const question=input.trim();
    if(!question||pending)return;
    const local:Turn={role:"user",content:question,mode:live?"LIVE_GPT_PUBLIC_RAG":"OFFLINE_GUIDED_DEMO"};
    const before=turns;
    setTurns(items=>[...items,local]);setInput("");setError("");
    if(!live){
      setTurns(items=>[...items,{role:"assistant",content:guidedAnswer(question,selected),mode:"OFFLINE_GUIDED_DEMO"}]);
      return;
    }
    if(!captcha){setError("Complete the human verification before sending a live GPT question.");setTurns(before);setInput(question);return;}
    setPending(true);
    try{
      const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),30000);
      let response:Response;
      try{response=await fetch(apiEndpoint+"/chat",{
        method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({case_id:selected,message:question,history:before.slice(-6).map(item=>({role:item.role,content:item.content.slice(0,900)})),turnstile_token:captcha}),
        signal:controller.signal,
      });}finally{clearTimeout(timer);}
      const data=await response.json().catch(()=>({error:"Invalid gateway response"}));
      if(!response.ok)throw new Error(data.error||"Live GPT unavailable");
      if(data.mode!=="LIVE_GPT_PUBLIC_RAG"||typeof data.answer!=="string")throw new Error("Invalid provider response mode");
      setTurns(items=>[...items,{role:"assistant",content:data.answer,mode:"LIVE_GPT_PUBLIC_RAG",refs:Array.isArray(data.references)?data.references:[]}]);
    }catch(e){setTurns(before);setInput(question);setError(e instanceof Error?e.message:"Could not reach GPT backend");}
    finally{setPending(false);setCaptcha("");if(idRef.current&&window.turnstile)window.turnstile.reset(idRef.current);}
  }
  return <div className="investigator-live">
    <div className="page-heading"><div><div className="eyebrow">AI PLATFORM / DISTRIBUTED DATA RELIABILITY</div><h1>Investigator</h1><p>Trace the originating fault across dependent systems. Investigate evidence, alternative hypotheses, and safe diagnostic checks.</p></div></div>
    <div className="preview-banner" role="note"><span>DATA PROVENANCE://</span> Public domain knowledge and synthetic incident context only. No private Elasticsearch, Kafka, Cloudera or real company telemetry is connected to this page.</div>
    <div className="investigator-live-top">
      <span className={`investigator-state ${live?"ready":"offline"}`}>{live?<Wifi size={15}/>:<WifiOff size={15}/>} {status==="CHECKING"?"Checking live GPT gateway":live?"LIVE GPT GATEWAY READY":status==="UNAVAILABLE"?"GPT gateway unavailable":"OFFLINE GUIDED PREVIEW"}</span>
      <span className="investigator-source">Root cause analysis · RAG · Read only</span>
      <button type="button" className="button" onClick={reset}><RotateCcw size={15}/> New conversation</button>
    </div>
    <div className="grid grid-2 investigator-live-layout">
      <section className="panel investigator-chat-panel">
        <div className="panel-heading"><h2><Bot size={19}/> Distributed Systems Investigator</h2><span>{live?"Generated GPT responses":"Deterministic guided walkthrough"}</span></div>
        <div className="panel-body investigator-thread" aria-live="polite" aria-relevant="additions">
          {turns.length===0&&<div className="investigator-welcome"><div className="investigator-hero-symbol"><Bot size={30}/></div><h3>Find where the incident really started.</h3><p>Kafka might report a timeout, while the originating fault is in a control plane, network dependency or downstream storage system.</p><div className="investigator-suggestions">{suggestedQuestions.map(q=><button key={q} onClick={()=>setInput(q)}>{q} →</button>)}</div></div>}
          {turns.map((turn,index)=><div key={index} className={`investigator-turn ${turn.role}`}><div className="investigator-turn-label">{turn.role==="user"?"YOU":turn.mode==="LIVE_GPT_PUBLIC_RAG"?"GPT · PUBLIC RAG":"OFFLINE GUIDED SAMPLE"}</div><div className="investigator-turn-text">{turn.content}</div>{turn.refs?.length? <div className="investigator-ref-list"><BookOpen size={15}/>{turn.refs.map(ref=>ref.url?<a key={ref.id} href={ref.url} target="_blank" rel="noreferrer" title={ref.title}>{ref.id} ↗</a>:<span key={ref.id} title={ref.title}>{ref.id}</span>)}</div>:null}</div>)}
          {pending&&<div className="investigator-turn assistant"><div className="investigator-turn-label">INVESTIGATING</div>Generating an evidence-aware explanation…</div>}
          <div ref={lastRef}/>
        </div>
        <div className="investigator-compose-shell">
          <form onSubmit={send}>
            <label className="investigator-input-label" htmlFor="investigator-question">Question for Investigator</label>
            <textarea id="investigator-question" className="form-input" placeholder="e.g. Kafka consumer lag grew after KRaft controller timeouts. What upstream cause should we test first?" maxLength={1200} rows={3} value={input} onChange={e=>setInput(e.target.value)} disabled={pending}/>
            {live&&<div className="investigator-challenge"><div ref={widgetRef}/><small>Turnstile verification is required for every real provider request.</small></div>}
            {challengeError&&<div className="investigator-error" role="alert">{challengeError}</div>}
            {error&&<div className="investigator-error" role="alert">{error}</div>}
            <div className="investigator-compose-footer"><span>{input.length}/1200 · {live?"Real GPT + curated KB":"No model calls or billing"}</span><button type="submit" className="button button-primary" disabled={pending||!input.trim()||(live&&!captcha)}>{pending?"Investigating…":live?"Ask GPT →":"Explore example →"}</button></div>
          </form>
        </div>
      </section>
      <div className="stack investigator-context">
        <section className="panel"><div className="panel-heading"><h2>Investigation scope</h2><span>Context-bound sample</span></div><div className="panel-body"><label className="investigator-input-label" htmlFor="investigator-scenario">Scenario</label><select id="investigator-scenario" className="form-input" value={selected} onChange={e=>changeScenario(e.target.value as CaseId)}>{scenarioOptions.map(x=><option key={x.key} value={x.key}>{x.title}</option>)}</select><p className="text-muted small">Changing scenario starts a new conversation. Actual enterprise systems are not connected.</p><div className="java-origin"><span>ORIGINATING-CAUSE HYPOTHESIS · SYNTHETIC</span><strong>{scenario.origin}</strong><p>{scenario.impact}</p></div></div></section>
        <section className="panel"><div className="panel-heading"><h2>Example evidence timeline</h2><span>{scenario.events.length} synthetic events</span></div><div className="panel-body"><ol className="java-timeline">{scenario.events.map(ev=><li className={`java-timeline-item ${ev.kind}`} key={ev.id}><span className="java-time">T + {ev.seconds}s</span><div><strong>{ev.service}</strong><p>{ev.description}</p><code>{ev.id}</code></div></li>)}</ol><a className="table-link" href="#/correlation">Inspect alternatives in Root Cause Explorer →</a></div></section>
        <section className="panel"><div className="panel-heading"><h2>Safety and knowledge</h2><ShieldCheck size={18}/></div><div className="panel-body"><p>Responses are grounded in an author-curated public troubleshooting reference library, not a fine-tuned model or proprietary operational data.</p><p>No approval, service restart, command execution or production troubleshooting action is offered here.</p><a className="table-link" target="_blank" rel="noreferrer" href="https://github.com/MINAADELMARKOS/Investinator-Enterprise-DataOps-Incident-Resolution/tree/main/services/public-investigator">Inspect gateway and reference corpus <ExternalLink size={13}/></a></div></section>
      </div>
    </div>
  </div>;
}
