package com.investinator.health;
import static com.investinator.health.HealthCheckResult.*;
import static com.investinator.domain.Json.*;
import com.investinator.evidence.*;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.zaxxer.hikari.HikariDataSource;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
@Component
public class ProbeCatalog {
    private final List<HealthProbe> probes;
    public List<HealthProbe> probes(){return probes;}
    private interface Check {HealthCheckResult run() throws Exception;}
    private HealthProbe probe(String name,String capability,boolean required,Check check){return new HealthProbe(){public String service(){return name;}public String capability(){return capability;}public boolean requiredForReadiness(){return required;}public HealthCheckResult check() throws Exception{return check.run();}};}
    public ProbeCatalog(Environment env,HealthSettings settings,ObjectProvider<HikariDataSource> source,ObjectProvider<EvidenceSearch> search,ObjectProvider<OpenSearchClient> client,ObjectProvider<DiagnosticProviders> extensions) {
        var map=new LinkedHashMap<String,HealthProbe>();var http=new ProbeHttp();
        map.put("java",probe("java","liveness-readiness-route",true,()->{
            String base=env.getProperty("HEALTH_SELF_URL","http://127.0.0.1:"+env.getProperty("server.port","8081"));
            for(String path:List.of("/actuator/health/liveness","/actuator/health/readiness","/api/v1/health")) {
                var r=http.get(base,path,Map.of());if(r.status()!=200)return fail("java",true,"Critical API route is not ready","HTTP_5XX");
                if(path.endsWith("/health")&&!"InvestiNator".equals(r.json().get("product")))return fail("java",true,"Unexpected critical API contract","CONTRACT_MISMATCH");
                if(!path.endsWith("/health")&&!"UP".equals(r.json().get("status")))return fail("java",true,"Application probe is not UP","PROBE_DOWN");
            }
            return ok("java","liveness-readiness-route","Local HTTP probes and critical API contract passed",true,Map.of());
        }));
        map.put("postgresql",probe("postgresql","authenticated-query-schema-pool",env.getProperty("investinator.persistence.enabled",Boolean.class,false),()->{
            var pool=source.getIfAvailable();if(pool==null)return absent("postgresql","Set the dedicated Java PostgreSQL settings");
            try(var c=pool.getConnection();var sql=c.createStatement()) {
                c.setReadOnly(true);sql.setQueryTimeout(2);
                try(var rows=sql.executeQuery("select 1")){if(!rows.next()||rows.getInt(1)!=1)return fail("postgresql",true,"Authenticated query failed","QUERY_FAILED");}
                int versions;try(var rows=sql.executeQuery("select count(*) from flyway_schema_history where success and type='SQL'")){rows.next();versions=rows.getInt(1);}
                var bean=pool.getHikariPoolMXBean();return of("postgresql","authenticated-query-schema-pool",versions>=2?Status.HEALTHY:Status.UNHEALTHY,SourceMode.LIVE,"authenticated PostgreSQL SELECT 1 and Flyway history",versions>=2?"Required schema is present":"Required migration is missing","Inspect migration history; never run competing migration owners",versions>=2?"":"SCHEMA_VERSION",true,obj("schema_migrations",versions,"pool_active",bean.getActiveConnections(),"pool_idle",bean.getIdleConnections(),"pool_max",pool.getMaximumPoolSize()));
            }
        }));
        map.put("nextjs",probe("nextjs","http-page-backend-proxy",false,()->{
            String base=env.getProperty("HEALTH_WEB_URL");if(base==null||base.isBlank())return absent("nextjs","Set HEALTH_WEB_URL");
            var page=http.get(base,"/",Map.of());var proxy=http.get(base,"/api/v1/health",Map.of());
            if(page.status()!=200||!page.body().contains("InvestiNator")||proxy.status()!=200||!"InvestiNator".equals(proxy.json().get("product")))return fail("nextjs",false,"Web page or backend proxy failed","HTTP_5XX");
            return ok("nextjs","http-page-backend-proxy","Page and actual backend proxy contract passed",false,Map.of());
        }));
        map.put("search",probe("search","cluster-mapping-search-freshness",false,()->{
            var engine=search.getIfAvailable();if(engine==null)return absent("search","Configure OpenSearch reader credentials and the evidence index");
            String tenant=env.getProperty("investinator.demo-tenant","demo");var health=engine.health(tenant);
            if(!"HEALTHY".equals(health.get("status")))return fail("search",false,"Authenticated cluster or mapping probe failed","SEARCH_UNAVAILABLE");
            var result=engine.search(tenant,"",null,Instant.now().minusSeconds(Math.min(14400,settings.freshnessSeconds)).toString(),null,0,1);
            if(!"HEALTHY".equals(result.get("status")))return fail("search",false,"Bounded tenant search failed","SEARCH_UNAVAILABLE");
            var items=(List<?>)result.get("items");if(items.isEmpty())return of("search","cluster-mapping-search-freshness",Status.DEGRADED,SourceMode.LIVE,"authenticated cluster/mapping and tenant query","No recent authorized evidence","Inspect collector flow and source timestamps","STALE_OR_EMPTY",false,Map.of());
            var item=(NormalizedEvidence)items.getFirst();long age=Duration.between(Instant.parse(item.timestamp()),Instant.now()).getSeconds();
            return of("search","cluster-mapping-search-freshness",age<=settings.freshnessSeconds?Status.HEALTHY:Status.DEGRADED,SourceMode.LIVE,"authenticated cluster/mapping and tenant query","Cluster, strict mapping and functional tenant search observed","Review stale/missing sources when applicable",age<=settings.freshnessSeconds?"":"STALE_EVIDENCE",false,obj("cluster_status",health.get("cluster_status"),"last_observed_at",item.timestamp(),"age_seconds",age,"data_source_mode",item.source_mode()));
        }));
        map.put("kafka",new KafkaHealthProbe(env,settings));
        map.put("otel",probe("otel","collector-and-trace-arrival",false,()->{
            String base=env.getProperty("HEALTH_OTEL_URL");if(base==null)return absent("otel","Set HEALTH_OTEL_URL and configure the trace backend");
            var reply=http.get(base,"/",Map.of());if(reply.status()!=200||!"Server available".equals(reply.json().get("status")))return fail("otel",false,"Collector health endpoint failed","HTTP_5XX");
            var backend=client.getIfAvailable();if(backend==null)return of("otel","collector-and-trace-arrival",Status.NOT_TESTED,SourceMode.LIVE,"collector HTTP health","Collector responds; export backend is not configured","Configure a retained trace backend","EXPORT_NOT_CONFIGURED",false,Map.of());
            var found=backend.search(s->s.index("investinator-traces").size(1),Map.class);
            if(found.hits().hits().isEmpty())return of("otel","collector-and-trace-arrival",Status.DEGRADED,SourceMode.LIVE,"collector HTTP and trace index","No trace batch has arrived","Send an explicit synthetic smoke span or inspect export errors","TRACE_NOT_OBSERVED",false,Map.of());
            var data=found.hits().hits().getFirst().source();if(data==null||!data.containsKey("resourceSpans"))return fail("otel",false,"Stored trace contract is invalid","TRACE_CONTRACT");
            return ok("otel","collector-and-trace-arrival","Collector responds and actual exported trace batch is present",false,obj("data_source_mode","SIMULATED_OR_EXTERNAL","trace_batch_observed",true));
        }));
        map.put("prometheus",probe("prometheus","readiness-targets-promql",false,()->{
            String base=env.getProperty("HEALTH_PROMETHEUS_URL");if(base==null)return absent("prometheus","Set HEALTH_PROMETHEUS_URL");
            if(http.get(base,"/-/ready",Map.of()).status()!=200)return fail("prometheus",false,"Prometheus readiness failed","HTTP_5XX");
            var targetResponse=http.get(base,"/api/v1/targets",Map.of());if(targetResponse.status()!=200)return fail("prometheus",false,"Scrape target API unavailable","HTTP_5XX");
            var targets=rows(map(targetResponse.json().get("data")).get("activeTargets"));
            if(targets.isEmpty()||targets.stream().anyMatch(t->!"up".equals(t.get("health"))))return fail("prometheus",false,"A configured scrape target is absent or down","SCRAPE_DOWN");
            var query=http.get(base,"/api/v1/query?query=up",Map.of());if(query.status()!=200||rows(map(query.json().get("data")).get("result")).isEmpty())return fail("prometheus",false,"Bounded PromQL query returned no samples","QUERY_EMPTY");
            return ok("prometheus","readiness-targets-promql","Readiness, active scrape targets and real PromQL samples passed",false,obj("targets",targets.size()));
        }));
        map.put("gpt",probe("gpt","configuration-only",false,()->{
            boolean key=present(env,"OPENAI_API_KEY"),model=present(env,"INVESTINATOR_OPENAI_MODEL")||present(env,"INVESTINATOR_OPENAI_FAST_MODEL")||present(env,"INVESTINATOR_OPENAI_REASONING_MODEL");
            return of("gpt","configuration-only",key&&model?Status.NOT_TESTED:Status.NOT_CONFIGURED,key&&model?SourceMode.NOT_TESTED:SourceMode.NOT_CONFIGURED,"backend configuration presence only",key&&model?"Configured; no paid API call was made":"Provider key/model is not configured","Only explicit ai-check --live may invoke a bounded paid probe","",false,obj("key_present",key,"model_present",model,"configured",key&&model,"paid_call_made",false));
        }));
        for(String name:List.of("spark","yarn","hdfs","hive","kudu","nifi","airflow","impala","zookeeper","cloudera-manager","atlas","ranger")) {
            String key="CONNECTOR_"+name.toUpperCase(Locale.ROOT).replace('-','_')+"_URL";
            map.put(name,probe(name,"diagnostic-connectivity",false,()->present(env,key)?of(name,"diagnostic-connectivity",Status.NOT_TESTED,SourceMode.NOT_TESTED,"configured endpoint metadata","Connector capability not yet verified","Enable a supported authenticated read-only adapter","CAPABILITY_NOT_TESTED",false,Map.of()):absent(name,"Set "+key+" and least-privilege authentication")));
        }
        extensions.orderedStream().forEach(provider->provider.probes().forEach(probe->map.put(probe.service(),probe)));
        probes=List.copyOf(map.values());
    }
    private boolean present(Environment env,String key){String value=env.getProperty(key);return value!=null&&!value.isBlank();}
    public static HealthCheckResult absent(String service,String next){return of(service,"configuration",Status.NOT_CONFIGURED,SourceMode.NOT_CONFIGURED,"backend configuration presence","Integration is not configured",next,"",false,Map.of());}
    public static HealthCheckResult ok(String service,String capability,String finding,boolean required,Map<String,Object> details){return of(service,capability,Status.HEALTHY,SourceMode.LIVE,"read-only service-specific diagnostic",finding,"No action required","",required,details);}
    public static HealthCheckResult fail(String service,boolean required,String finding,String error){return of(service,"functional",Status.UNHEALTHY,SourceMode.LIVE,"read-only service-specific diagnostic",finding,"Inspect the service, scoped credentials and diagnostics",error,required,Map.of());}
}
