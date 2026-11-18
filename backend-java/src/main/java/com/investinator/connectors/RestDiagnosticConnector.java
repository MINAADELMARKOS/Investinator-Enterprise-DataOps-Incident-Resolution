package com.investinator.connectors;
import static com.investinator.health.HealthCheckResult.*;
import static com.investinator.domain.Json.*;
import com.investinator.health.*;
import java.util.*;
import java.time.*;

/** Fixed, documented REST reads. Only selected aggregate facts leave the adapter. */
public class RestDiagnosticConnector implements DiagnosticConnector {
    private final ConnectorConfig config;private final ConnectorHttp http;
    public RestDiagnosticConnector(ConnectorConfig config){this(config,new ConnectorHttp());}
    public RestDiagnosticConnector(ConnectorConfig config,ConnectorHttp http){this.config=config;this.http=http;}
    public String service(){return config.service;}
    public Descriptor descriptor(){return new Descriptor(service(),"HTTPS GET",versionContract(),"Bearer, Basic, JVM mTLS or SPNEGO",Set.of(Capability.health,Capability.metrics,Capability.config,Capability.metadata,Capability.readOnlyChecks),List.of("No raw log export or automatic topology inference","Only bounded aggregate service facts are exposed","Private live clusters require independently scoped backend credentials"));}
    private String versionContract(){return switch(service()){case "spark"->"Spark monitoring REST v1, Spark 3.x/4.x";case "yarn","hdfs"->"Hadoop 3.x";case "nifi"->"NiFi 1.x/2.x";case "airflow"->"Airflow 2.x REST v1 only";case "cloudera-manager"->"CM REST v40+ / product 7.x";case "atlas"->"Atlas v2 REST / product 2.x";case "ranger"->"Ranger REST / product 2.x";case "impala"->"Impala 4.x debug web JSON";default->"UNSUPPORTED";};}
    private Map<String,Object> get(String path)throws Exception {
        var reply=http.get(config,path);if(reply.status()==401||reply.status()==403)throw new ProbeFailure("AUTHORIZATION");if(reply.status()>=500)throw new ProbeFailure("HTTP_5XX");if(reply.status()!=200)throw new ProbeFailure("HTTP_CONTRACT");return reply.json();
    }
    private List<Map<String,Object>> array(String path)throws Exception {var reply=http.get(config,path);if(reply.status()==401||reply.status()==403)throw new ProbeFailure("AUTHORIZATION");if(reply.status()!=200)throw new ProbeFailure(reply.status()>=500?"HTTP_5XX":"HTTP_CONTRACT");Object decoded=decode(reply.body());if(!(decoded instanceof List<?>))throw new ProbeFailure("CONTRACT_MISMATCH");return rows(decoded);}
    public HealthCheckResult check() {
        if(!config.configured())return ProbeCatalog.absent(service(),"Set "+config.prefix+"URL and scoped authentication");
        if(!config.authenticationConfigured())return ProbeCatalog.absent(service(),"Provide "+config.prefix+" authentication through backend secrets");
        try {
            config.endpoint();var facts=new LinkedHashMap<String,Object>();var gaps=new ArrayList<String>();boolean bad=false;String version;
            int max=config.limit("MAX_ITEMS",100,1000);
            switch(service()) {
                case "spark" -> {
                    version=str(get("/api/v1/version"),"spark");requireVersion(version,"3.","4.");
                    var apps=array("/api/v1/applications?limit="+max);if(apps.size()>max)throw new ProbeFailure("RESPONSE_LIMIT");facts.put("applications",apps.size());int failed=0,dead=0,active=0;
                    for(var app:apps.stream().limit(config.limit("MAX_APPS",3,10)).toList()) {
                        String id=str(app,"id");if(!id.matches("[A-Za-z0-9_.-]{1,150}"))throw new ProbeFailure("INVALID_ID");
                        var executors=array("/api/v1/applications/"+id+"/executors");var stages=array("/api/v1/applications/"+id+"/stages?limit="+max);
                        if(executors.size()>max||stages.size()>max)throw new ProbeFailure("RESPONSE_LIMIT");
                        failed+=stages.stream().filter(s->"FAILED".equals(s.get("status"))).count();
                        for(var executor:executors){if(Boolean.FALSE.equals(executor.get("isActive")))dead++;long memory=number(executor.get("memoryUsed")),limit=number(executor.get("maxMemory"));if(limit>0&&100*memory/limit>=config.limit("MAX_MEMORY_PERCENT",90,100))bad=true;}
                        active+=executors.stream().filter(e->Boolean.TRUE.equals(e.get("isActive"))).count();
                    }
                    facts.putAll(obj("failed_stages",failed,"inactive_executors",dead,"active_executors",active));bad|=failed>0||dead>0;
                    if(apps.size()>config.limit("MAX_APPS",3,10))gaps.add("Application sampling limit reached");gaps.add("Streaming backlog/dependency error details require a configured streaming metrics source; not inferred from REST availability");
                }
                case "yarn" -> {
                    var info=map(get("/ws/v1/cluster/info").get("clusterInfo"));version=str(info,"hadoopVersion");requireVersion(version,"3.");
                    var m=map(get("/ws/v1/cluster/metrics").get("clusterMetrics"));requireFields(m,"appsPending","availableMB","containersPending");
                    var apps=rows(map(get("/ws/v1/cluster/apps?states=ACCEPTED,RUNNING,FAILED&limit="+max).get("apps")).get("app"));
                    var scheduler=map(get("/ws/v1/cluster/scheduler").get("scheduler"));if(scheduler.isEmpty())throw new ProbeFailure("CONTRACT_MISMATCH");
                    int stuck=0;for(var app:apps){if("FAILED".equals(app.get("state")))bad=true;if("ACCEPTED".equals(app.get("state"))&&System.currentTimeMillis()-number(app.get("startedTime"))>config.limit("MAX_PENDING_SECONDS",600,86400)*1000L)stuck++;}
                    facts.putAll(obj("ha_state",info.getOrDefault("haState","UNKNOWN"),"pending_apps",m.get("appsPending"),"pending_containers",m.get("containersPending"),"available_mb",m.get("availableMB"),"stuck_apps",stuck,"observed_apps",apps.size()));
                    bad|=(!"ACTIVE".equals(info.get("haState")))||number(m.get("availableMB"))==0||number(m.get("containersPending"))>config.limit("MAX_PENDING_CONTAINERS",100,1000000)||stuck>0;
                }
                case "hdfs" -> {
                    var beans=rows(get("/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo").get("beans"));if(beans.size()!=1)throw new ProbeFailure("CONTRACT_MISMATCH");var info=beans.getFirst();version=str(info,"Version").split(",")[0];requireVersion(version,"3.");
                    var states=rows(get("/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus").get("beans"));if(states.size()!=1)throw new ProbeFailure("CONTRACT_MISMATCH");String state=str(states.getFirst(),"State");
                    requireFields(info,"NumberOfMissingBlocks","NumberOfMissingBlocksWithReplicationFactorOne","LiveNodes","DeadNodes","Safemode");
                    var fs=rows(get("/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem").get("beans"));if(fs.size()!=1)throw new ProbeFailure("CONTRACT_MISMATCH");requireFields(fs.getFirst(),"UnderReplicatedBlocks","CorruptBlocks");
                    long missing=number(info.get("NumberOfMissingBlocks")),under=number(fs.getFirst().get("UnderReplicatedBlocks")),corrupt=number(fs.getFirst().get("CorruptBlocks"));
                    int live=map(decode(str(info,"LiveNodes"))).size(),dead=map(decode(str(info,"DeadNodes"))).size();boolean safe=!str(info,"Safemode").isBlank();
                    facts.putAll(obj("ha_state",state,"missing_blocks",missing,"corrupt_blocks",corrupt,"under_replicated_blocks",under,"live_datanodes",live,"dead_datanodes",dead,"safe_mode",safe));
                    bad|=!Set.of("active","standby").contains(state)||missing>0||corrupt>0||under>config.threshold("MAX_UNDER_REPLICATED_BLOCKS",0,1000000)||dead>0||live==0||safe;gaps.add("JMX read latency is measured by runner; it does not measure HDFS data-read latency or prove a paired NameNode is healthy");
                }
                case "nifi" -> {
                    version=str(map(get("/nifi-api/flow/about").get("about")),"version");requireVersion(version,"1.","2.");
                    var diagnostics=map(map(get("/nifi-api/system-diagnostics").get("systemDiagnostics")).get("aggregateSnapshot"));requireFields(diagnostics,"uptime","availableProcessors");
                    var status=map(map(get("/nifi-api/flow/process-groups/root/status?recursive=false").get("processGroupStatus")).get("aggregateSnapshot"));requireFields(status,"flowFilesQueued","bytesQueued","activeThreadCount");
                    var bulletins=rows(map(get("/nifi-api/flow/bulletin-board?limit="+max).get("bulletinBoard")).get("bulletins"));int errors=0;for(var b:bulletins){String level=str(map(b.get("bulletin")),"level");if(Set.of("ERROR","WARN").contains(level))errors++;}
                    facts.putAll(obj("queued_flow_files",status.get("flowFilesQueued"),"queued_bytes",status.get("bytesQueued"),"active_threads",status.get("activeThreadCount"),"warning_bulletins",errors));
                    bad|=errors>0||number(status.get("flowFilesQueued"))>config.limit("MAX_QUEUE_COUNT",10000,1000000000);gaps.add("Queue age/backpressure requires connection-specific permissions; clustered per-node state is not inferred from aggregate status");
                }
                case "airflow" -> {
                    version=str(get("/api/v1/version"),"version");requireVersion(version,"2.");var h=get("/api/v1/health");
                    for(String component:List.of("metadatabase","scheduler"))if(!"healthy".equals(map(h.get(component)).get("status")))bad=true;
                    var dags=rows(get("/api/v1/dags?limit="+max+"&only_active=true").get("dags"));int failures=0;String dag=config.value("DAG_ID");
                    if(!dag.isBlank()){if(!dag.matches("[A-Za-z0-9_.-]{1,150}"))throw new ProbeFailure("INVALID_ID");var runs=rows(get("/api/v1/dags/"+dag+"/dagRuns?limit="+max+"&order_by=-execution_date").get("dag_runs"));for(var run:runs)if("failed".equals(run.get("state")))failures++;}
                    else gaps.add("DAG/task retry, SLA and freshness checks need a scoped DAG_ID; worker state is not implied by scheduler health");
                    facts.putAll(obj("active_dags",dags.size(),"failed_sampled_runs",failures,"scheduler_healthy",!bad));bad|=failures>0;
                }
                case "cloudera-manager" -> {
                    var reply=http.get(config,"/api/version");if(reply.status()!=200)throw new ProbeFailure("AUTHORIZATION");String api=reply.body().replace("\"","").trim();if(!api.matches("v[0-9]+")||Integer.parseInt(api.substring(1))<40)throw new ProbeFailure("UNSUPPORTED_VERSION");
                    var cm=get("/api/"+api+"/cm/version");version=str(cm,"version");requireVersion(version,"7.");var clusters=rows(get("/api/"+api+"/clusters?view=summary").get("items"));facts.put("clusters",clusters.size());gaps.add("No service health inferred from CM process connectivity; per-cluster service selection required");
                }
                case "atlas" -> {
                    version=str(get("/api/atlas/admin/version"),"Version");requireVersion(version,"2.");String state=str(get("/api/atlas/admin/status"),"Status");facts.put("server_state",state);bad|=!"ACTIVE".equals(state);gaps.add("Entity/data lineage freshness is not implied by server status");
                }
                case "ranger" -> {
                    return of(service(),capability(),Status.NOT_TESTED,SourceMode.NOT_TESTED,"backend configured endpoint","Ranger distribution/version contract has not been verified","Use a supported distribution-specific version adapter before any policy metadata read","UNSUPPORTED_CAPABILITY",false,Map.of());
                }
                case "impala" -> {
                    var info=get("/?json");version=str(info,"version");requireVersion(version,"impalad version 4.","4.");var backends=get("/backends?json");facts.put("known_backends",number(backends.get("num_known_backends")));bad|=number(backends.get("num_active_backends"))<1;gaps.add("Debug web JSON is distribution-specific; query responsiveness requires a separately configured read-only JDBC endpoint");
                }
                default -> throw new ProbeFailure("UNSUPPORTED_CAPABILITY");
            }
            facts.put("data_source_mode",config.flag("LOCAL_FIXTURE")?"SIMULATED":"EXTERNAL");facts.put("server_version",version);facts.put("auth_mode",config.auth());facts.put("capability_gaps",gaps);
            return of(service(),capability(),bad?Status.DEGRADED:Status.HEALTHY,SourceMode.LIVE,"authenticated service-specific version and semantic GET APIs",bad?"Observed semantic thresholds require attention":"Supported version and scoped semantic reads passed","Inspect recorded aggregate facts and capability gaps; no automatic action",bad?"SEMANTIC_THRESHOLD":"",false,facts);
        }catch(ProbeFailure error){return ProbeCatalog.fail(service(),false,"Service contract, authorization or version check failed",error.kind);}
        catch(Exception error){return ProbeCatalog.fail(service(),false,"Read-only connector did not complete successfully",error.getClass().getSimpleName());}
    }
    private static String str(Map<String,Object> value,String key){Object result=value.get(key);return result==null?"":result.toString();}
    private static long number(Object value){return value instanceof Number n?n.longValue():0;}
    private static void requireFields(Map<String,Object> value,String... keys)throws ProbeFailure{for(String key:keys)if(!value.containsKey(key))throw new ProbeFailure("CONTRACT_MISMATCH");}
    private static void requireVersion(String version,String... prefixes)throws ProbeFailure{if(Arrays.stream(prefixes).noneMatch(version::startsWith))throw new ProbeFailure("UNSUPPORTED_VERSION");}
    private static class ProbeFailure extends Exception {final String kind;ProbeFailure(String kind){this.kind=kind;}}
}
