/** Actual local public preview smoke; no deployment, provider or private service request. */
import {createRequire} from 'node:module';
import {spawn} from 'node:child_process';
import {createServer} from 'node:net';
import {fileURLToPath} from 'node:url';
import {resolve,dirname} from 'node:path';
import {mkdirSync,writeFileSync} from 'node:fs';
const root=fileURLToPath(new URL('../',import.meta.url));
const require=createRequire(new URL('../apps/web/package.json',import.meta.url));
const {chromium}=require('@playwright/test');
const socket=createServer();await new Promise(r=>socket.listen(0,'127.0.0.1',r));
const port=socket.address().port;await new Promise(r=>socket.close(r));
const origin=`http://127.0.0.1:${port}`;
const repo=process.env.GITHUB_REPOSITORY?.split('/')[1];
const base=repo?`/${repo}/`:'/';
const server=spawn(process.execPath,[resolve(root,'apps/showcase/node_modules/vite/bin/vite.js'),'preview','--host','127.0.0.1','--port',String(port),'--strictPort'],{cwd:resolve(root,'apps/showcase'),stdio:'ignore'});
let browser;let outsideBlocked=0;
const start=Date.now();const checks=[];
try{
 let ready=false;
 for(let i=0;i<100;i++){try{const r=await fetch(origin+base);if(r.ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,100));}
 if(!ready)throw Error('Owned static preview readiness deadline');
 browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
 const context=await browser.newContext();
 await context.route('**/*',route=>{if(new URL(route.request().url()).origin===origin)return route.continue();outsideBlocked++;return route.abort();});
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.name));
 await page.goto(origin+base+'#/system-health');await page.waitForLoadState('networkidle');
 if(!/simulat/i.test(await page.locator('body').innerText()))throw Error('Simulation provenance missing');checks.push('simulated operational view');
 await page.goto(origin+base+'#/investigator');
 const ask=async()=>{
  await page.locator('#investigator-question').fill('Kafka lag followed KRaft controller timeouts. Which upstream cause should be checked?');
  await page.getByRole('button',{name:/Explore example/}).click();
  await page.getByText('OFFLINE GUIDED EXAMPLE',{exact:false}).first().waitFor();
  if(errors.length)throw Error('Public preview raised a runtime error');
 };
 await ask();checks.push('native browser offline answer explicitly not GPT');
 await page.keyboard.press('Tab');const focused=await page.evaluate(()=>document.activeElement?.tagName);
 if(!['A','BUTTON','INPUT','TEXTAREA','SELECT'].includes(focused))throw Error('Keyboard focus target missing');checks.push('keyboard focus');
 // Browser implementations may return a Promise from scrollIntoView. This is a
 // separately labeled protocol fixture; React effect cleanup must remain undefined.
 await page.addInitScript(()=>{const original=Element.prototype.scrollIntoView;Element.prototype.scrollIntoView=function(...args){original.apply(this,args);return Promise.resolve();};});
 await page.reload();await ask();checks.push('promise-return scroll protocol fixture does not become effect cleanup');
 const output=process.argv.includes('--output')?process.argv[process.argv.indexOf('--output')+1]:resolve(root,'.runtime/showcase-acceptance.json');
 mkdirSync(dirname(output),{recursive:true});
 writeFileSync(output,JSON.stringify({status:'PASS_LOCAL_BROWSER',checks:checks.length,checks_executed:checks,source_mode:'SIMULATED_PUBLIC_UI_ON_ACTUAL_LOCAL_BROWSER',browser_version:browser.version(),outside_requests_blocked:outsideBlocked,model_calls:0,deployment_tested:false,formal_accessibility_certificate:'NOT_TESTED',promise_return_case:'EXPLICIT_PROTOCOL_FIXTURE',elapsed_seconds:(Date.now()-start)/1000},null,2)+'\n');
 console.log(JSON.stringify({status:'PASS',checks:checks.length,model_calls:0,deployment_tested:false}));
}finally{if(browser)await browser.close();server.kill('SIGTERM');}
