package com.investinator.correlation;
import java.time.*;
import java.util.*;
/** Ranking is deterministic, bounded and explicitly non-causal; observations are data, never instructions. */
public final class CorrelationEngine {
    public enum Signal { NETWORK_UNSTABLE,QUORUM_UNAVAILABLE,ZK_SESSION_EXPIRED,DISK_EXHAUSTED,BROKER_ERROR,CONSUMER_LAG,SPARK_DELAY,DATASET_STALE,HDFS_HA_UNAVAILABLE,HDFS_MISSING_BLOCKS,KUDU_TABLET_UNAVAILABLE,NIFI_BACKPRESSURE,SPARK_EXECUTOR_OOM,HIVE_METADATA_FAILURE,SPARK_RETRY_STORM,YARN_QUEUE_SATURATED,KUDU_TSERVER_FAILURE,HIVE_PARTITION_MISSING,ROLLOUT_CONFLICT,UNKNOWN }
    public enum State { ABNORMAL,HEALTHY,RECOVERED }
    public record Observation(String id,String tenant_id,String incident_id,String entity_id,Signal signal,State state,String observed_at,String source_mode,double intensity,String signature) {
        public Observation {if(id==null||id.isBlank()||id.length()>150||tenant_id==null||incident_id==null||entity_id==null||signal==null||state==null||!Set.of("SIMULATED","LIVE").contains(source_mode)||!Double.isFinite(intensity)||intensity<0||intensity>1||signature==null||signature.length()>500)throw new IllegalArgumentException("Invalid observation");Instant.parse(observed_at);}
    }
    public record Reference(String evidence_id,String entity_id,String observed_at,String reason) {}
    public record Candidate(String entity_id,String signal,String status,String confidence,List<String> confidence_reasons,List<Reference> supporting,List<Reference> contradicting,List<String> impact_radius,List<String> next_read_only_checks,List<String> prevention_opportunities,String risk) {}
    public record Investigation(String incident_id,String graph_id,int graph_revision,String metadata_mode,String platform_version,String source_mode,List<Observation> observed_symptoms,List<Candidate> candidate_causes,List<String> unknowns,List<String> impact_radius,List<String> next_read_only_checks,List<String> prevention_opportunities,String risk,String confirmed_cause) {}
    private final Duration skew,window;
    public CorrelationEngine(){this(Duration.ofSeconds(30),Duration.ofHours(4));}
    public CorrelationEngine(Duration skew,Duration window){if(skew.isNegative()||skew.compareTo(Duration.ofMinutes(5))>0||window.isNegative()||window.isZero()||window.compareTo(Duration.ofHours(4))>0)throw new IllegalArgumentException("Invalid correlation bounds");this.skew=skew;this.window=window;}
    public static Signal normalize(String text){String value=text.toLowerCase(Locale.ROOT);if(value.contains("no active namenode"))return Signal.HDFS_HA_UNAVAILABLE;if(value.contains("missing blocks"))return Signal.HDFS_MISSING_BLOCKS;if(value.contains("tablet unavailable"))return Signal.KUDU_TABLET_UNAVAILABLE;if(value.contains("backpressure"))return Signal.NIFI_BACKPRESSURE;if(value.contains("outofmemory")||value.contains("executor oom"))return Signal.SPARK_EXECUTOR_OOM;if(value.contains("metastore unavailable")||value.contains("metadata failure"))return Signal.HIVE_METADATA_FAILURE;if(value.contains("diskerror")||value.contains("no space left")||value.contains("disk exhausted"))return Signal.DISK_EXHAUSTED;if(value.contains("sessionexpired")||value.contains("session expired"))return Signal.ZK_SESSION_EXPIRED;if(value.contains("quorum")&&(value.contains("no leader")||value.contains("unavailable")))return Signal.QUORUM_UNAVAILABLE;if(value.contains("network")&&(value.contains("unstable")||value.contains("packet loss")))return Signal.NETWORK_UNSTABLE;return Signal.UNKNOWN;}
    private record Ranked(int score,Candidate value) {}
    public Investigation investigate(String tenant,String incident,DependencyGraph graph,List<Observation> supplied) {
        if(supplied.size()>1000)throw new IllegalArgumentException("Observation limit");
        var unknowns=new LinkedHashSet<String>();unknowns.add("Temporal/graph association is not proof of causation; no cause is automatically confirmed");
        if(graph.metadata_mode().equals("UNKNOWN"))unknowns.add("Kafka metadata mode unverified; no coordination-chain hypothesis enabled");
        var byId=new LinkedHashMap<String,Observation>();for(var o:supplied){if(!tenant.equals(o.tenant_id)||!incident.equals(o.incident_id))throw new IllegalArgumentException("Cross-tenant/incident evidence denied");var old=byId.putIfAbsent(o.id,o);if(old!=null&&!old.equals(o))throw new IllegalArgumentException("Conflicting evidence ID");}
        var observations=byId.values().stream().sorted(Comparator.comparing(Observation::observed_at).thenComparing(Observation::id)).toList();
        if(observations.isEmpty())unknowns.add("No authorized observations available");
        var nodes=new HashSet<String>();graph.nodes().forEach(n->nodes.add(n.id()));
        var ranked=new ArrayList<Ranked>();
        for(var origin:observations) {
            if(origin.state!=State.ABNORMAL||!Set.of(Signal.NETWORK_UNSTABLE,Signal.QUORUM_UNAVAILABLE,Signal.ZK_SESSION_EXPIRED,Signal.DISK_EXHAUSTED,Signal.BROKER_ERROR,Signal.HDFS_HA_UNAVAILABLE,Signal.HDFS_MISSING_BLOCKS,Signal.KUDU_TABLET_UNAVAILABLE,Signal.NIFI_BACKPRESSURE,Signal.SPARK_EXECUTOR_OOM,Signal.HIVE_METADATA_FAILURE,Signal.SPARK_RETRY_STORM,Signal.YARN_QUEUE_SATURATED,Signal.KUDU_TSERVER_FAILURE,Signal.HIVE_PARTITION_MISSING).contains(origin.signal))continue;
            if(origin.signal==Signal.ZK_SESSION_EXPIRED&&!graph.metadata_mode().equals("LEGACY_ZOOKEEPER")||origin.signal==Signal.QUORUM_UNAVAILABLE&&!graph.metadata_mode().equals("KRAFT")){unknowns.add("Ignored incompatible coordination observation "+origin.id);continue;}
            if(!graph.source_mode().equals(origin.source_mode)){unknowns.add("Source-mode mismatch; simulated and live evidence are not combined");continue;}
            if(!nodes.contains(origin.entity_id)){unknowns.add("Missing graph node for "+origin.id);continue;}
            var descendants=graph.downstream(origin.entity_id,Instant.parse(origin.observed_at));var support=new ArrayList<Reference>();var contradiction=new ArrayList<Reference>();var components=new HashSet<String>();boolean recovery=false;Instant start=Instant.parse(origin.observed_at);
            for(var o:observations) {
                if(!o.source_mode.equals(origin.source_mode)){unknowns.add("Source-mode mismatch; simulated and live evidence are not combined");continue;}
                Instant time=Instant.parse(o.observed_at);if(Duration.between(start,time).abs().compareTo(window)>0)continue;
                if(o.entity_id.equals(origin.entity_id)&&o.signal==origin.signal&&o.state==State.HEALTHY&&Duration.between(start,time).abs().compareTo(skew)<=0)contradiction.add(ref(o,"Same-capability healthy observation contradicts candidate"));
                if(o.entity_id.equals(origin.entity_id)&&o.signal==origin.signal&&o.state==State.RECOVERED&&!time.isBefore(start)){recovery=true;support.add(ref(o,"Origin recovery observed; recovery alone does not prove cause"));}
                if(o.state!=State.ABNORMAL||!descendants.contains(o.entity_id))continue;
                // A downstream observation must be reachable using topology known at its own timestamp too.
                if(!graph.downstream(origin.entity_id,time).contains(o.entity_id))continue;
                if(time.isBefore(start.minus(skew)))contradiction.add(ref(o,"Downstream failure predates candidate beyond clock-skew tolerance"));
                else{support.add(ref(o,o.id.equals(origin.id)?"Direct originating symptom":"Directionally reachable symptom follows within skew/window"));components.add(o.entity_id);}
            }
            var reasons=new ArrayList<String>();reasons.add("Distinct reachable components: "+components.size()+" (repeated symptom count does not raise confidence)");reasons.add("Edge validity and discovery time checked; skew tolerance "+skew.toSeconds()+" seconds");
            if(recovery)reasons.add("Recovery temporal pattern available, not an intervention proof");
            String confidence=contradiction.isEmpty()&&components.size()>=3?"CORROBORATED_HYPOTHESIS":"LIMITED_HYPOTHESIS";
            int score=Math.min(5,components.size())*10+(int)(origin.intensity*5)+(recovery?2:0)-contradiction.size()*100;
            if(support.stream().noneMatch(r->r.evidence_id.equals(origin.id)))continue;
            var candidate=new Candidate(origin.entity_id,origin.signal.name(),"HYPOTHESIS",confidence,List.copyOf(reasons),List.copyOf(support),List.copyOf(contradiction),descendants.stream().filter(n->!n.equals(origin.entity_id)).sorted().toList(),List.of("Inspect scoped "+origin.signal+" metrics and contemporaneous healthy counterevidence","Verify source clocks and graph provenance"),List.of("Monitor originating capability and downstream freshness","Review redundancy/capacity based on verified observations"),contradiction.isEmpty()?"Unconfirmed causal attribution; no automatic remediation":"Conflicting evidence; do not act on this attribution");
            // One candidate per entity/signature: duplicated source lines cannot vote multiple times.
            String key=origin.entity_id+":"+origin.signal;var existing=ranked.stream().filter(r->(r.value.entity_id+":"+r.value.signal).equals(key)).findFirst();if(existing.isEmpty()||existing.get().score<score){existing.ifPresent(ranked::remove);ranked.add(new Ranked(score,candidate));}
        }
        ranked.sort(Comparator.comparingInt(Ranked::score).reversed().thenComparing(r->r.value.entity_id).thenComparing(r->r.value.signal));
        var candidates=ranked.stream().limit(20).map(Ranked::value).toList();if(candidates.isEmpty())unknowns.add("Insufficient compatible upstream observations; no originating candidate selected");
        var modes=new HashSet<String>();observations.forEach(o->modes.add(o.source_mode));modes.add(graph.source_mode());String mode=modes.size()==1?modes.iterator().next():"MIXED";
        var impact=candidates.isEmpty()?List.<String>of():candidates.getFirst().impact_radius;
        return new Investigation(incident,graph.graph_id(),graph.revision(),graph.metadata_mode(),graph.platform_version(),mode,observations,candidates,List.copyOf(unknowns),impact,List.of("Gather missing scoped metrics without changing services"),List.of("Validate causal claims against healthy and recovery counterexamples"),"Hypotheses only; independent authorization and verification required for any production change",null);
    }
    private Reference ref(Observation o,String reason){return new Reference(o.id,o.entity_id,o.observed_at,reason);}
}
