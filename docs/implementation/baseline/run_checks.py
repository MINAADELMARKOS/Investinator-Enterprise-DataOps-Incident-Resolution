import subprocess,json,os,sys
from pathlib import Path
root=Path.cwd(); out=root/'docs/implementation/baseline'
checks=[('pytest',['.venv/bin/pytest','-q'],root),('ruff',['.venv/bin/ruff','check','src','tests'],root),('mypy',['.venv/bin/mypy','src'],root),('evaluation',['.venv/bin/python','-m','incident_copilot.evaluation','eval/test_incidents.jsonl'],root),('compose',['docker','compose','config','--quiet'],root),('web-build',['npm','run','build'],root/'apps/web'),('web-audit',['npm','audit','--audit-level=high','--json','--cache','/workspace/.cache/npm'],root/'apps/web'),('showcase-audit',['npm','audit','--audit-level=high','--json','--cache','/workspace/.cache/npm'],root/'apps/showcase')]
results=[]
for name,cmd,cwd in checks:
 p=subprocess.run(cmd,cwd=cwd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (out/(name+'.txt')).write_text(p.stdout); results.append({'check':name,'command':cmd,'cwd':str(cwd.relative_to(root)),'exit_code':p.returncode}); print(name,p.returncode,flush=True)
(out/'checks.json').write_text(json.dumps(results,indent=2)+'\n')
