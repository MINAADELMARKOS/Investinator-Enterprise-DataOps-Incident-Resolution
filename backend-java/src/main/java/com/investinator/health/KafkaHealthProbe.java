package com.investinator.health;
import static com.investinator.health.HealthCheckResult.*;
import static com.investinator.domain.Json.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.TopicPartition;
import org.springframework.core.env.Environment;
import java.util.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Admin-only diagnostics. This class contains no producer, topic mutations or execution API. */
public class KafkaHealthProbe implements HealthProbe {
    private final Environment env;private final HealthSettings settings;
    public KafkaHealthProbe(Environment env,HealthSettings settings){this.env=env;this.settings=settings;}
    public String service(){return "kafka";}public String capability(){return "broker-partitions-lag-metadata-mode";}
    public static Map<String,Object> configuration(Environment env) {
        var props=new HashMap<String,Object>();props.put("bootstrap.servers",env.getProperty("KAFKA_BOOTSTRAP_SERVERS",""));props.put("request.timeout.ms",2000);props.put("default.api.timeout.ms",2500);
        var keys=Map.of("KAFKA_SECURITY_PROTOCOL","security.protocol","KAFKA_SASL_MECHANISM","sasl.mechanism","KAFKA_SASL_JAAS_CONFIG","sasl.jaas.config","KAFKA_SSL_TRUSTSTORE_LOCATION","ssl.truststore.location","KAFKA_SSL_TRUSTSTORE_PASSWORD","ssl.truststore.password","KAFKA_SSL_KEYSTORE_LOCATION","ssl.keystore.location","KAFKA_SSL_KEYSTORE_PASSWORD","ssl.keystore.password");
        keys.forEach((key,target)->{String value=env.getProperty(key);if(value!=null&&!value.isBlank())props.put(target,value);});return props;
    }
    protected Admin openAdmin(Map<String,Object> props){return Admin.create(props);}
    public HealthCheckResult check() throws Exception {
        var props=configuration(env);if(props.get("bootstrap.servers").toString().isBlank())return ProbeCatalog.absent("kafka","Set KAFKA_BOOTSTRAP_SERVERS and scoped Kafka authentication");
        Admin admin=openAdmin(props);
        try {
            var cluster=admin.describeCluster();var nodes=cluster.nodes().get(2,TimeUnit.SECONDS);
            if(nodes.isEmpty())return ProbeCatalog.fail("kafka",false,"No broker was discovered","BROKER_MISSING");
            String clusterId=cluster.clusterId().get(2,TimeUnit.SECONDS);int broker=nodes.iterator().next().id();
            var resource=new ConfigResource(ConfigResource.Type.BROKER,Integer.toString(broker));
            var config=admin.describeConfigs(List.of(resource)).all().get(2,TimeUnit.SECONDS).get(resource);
            String roles=config.get("process.roles")==null?"":Objects.toString(config.get("process.roles").value(),"");
            boolean legacy=config.get("zookeeper.connect")!=null&&config.get("zookeeper.connect").value()!=null&&!config.get("zookeeper.connect").value().isBlank();
            String mode=!roles.isBlank()?"KRAFT":legacy?"LEGACY_ZOOKEEPER":"UNKNOWN";
            var details=obj("cluster_id",clusterId,"brokers",nodes.size(),"metadata_mode",mode,"mode_evidence",!roles.isBlank()?"broker process.roles":legacy?"broker zookeeper.connect presence":"insufficient broker configuration evidence");
            var version=config.get("inter.broker.protocol.version");details.put("server_protocol_version",version==null?"UNKNOWN":version.value());
            boolean degraded=mode.equals("UNKNOWN");
            if(mode.equals("KRAFT")) {
                var quorum=admin.describeMetadataQuorum().quorumInfo().get(2,TimeUnit.SECONDS);
                details.put("quorum_leader",quorum.leaderId());details.put("quorum_voters",quorum.voters().size());details.put("quorum_high_watermark",quorum.highWatermark());
                if(quorum.leaderId()<0||quorum.voters().isEmpty())return ProbeCatalog.fail("kafka",false,"KRaft metadata quorum has no elected leader/voters","QUORUM_UNAVAILABLE");
            }
            var allTopics=admin.listTopics().names().get(2,TimeUnit.SECONDS).stream().sorted().toList();
            var topics=allTopics.stream().limit(settings.maxTopics).toList();degraded|=allTopics.size()>topics.size();
            int offline=0,under=0,partitions=0;
            if(!topics.isEmpty())for(var topic:admin.describeTopics(topics).allTopicNames().get(2,TimeUnit.SECONDS).values())for(var partition:topic.partitions()) {
                partitions++;if(partition.leader()==null||partition.leader().id()<0)offline++;
                if(partition.isr().size()<partition.replicas().size())under++;
            }
            var allGroups=admin.listConsumerGroups().valid().get(2,TimeUnit.SECONDS).stream().map(ConsumerGroupListing::groupId).sorted().toList();
            var groups=allGroups.stream().limit(settings.maxGroups).toList();degraded|=allGroups.size()>groups.size();
            long maxLag=0;boolean lagObserved=false;
            if(!groups.isEmpty()) {
                var spec=new LinkedHashMap<String,ListConsumerGroupOffsetsSpec>();groups.forEach(group->spec.put(group,new ListConsumerGroupOffsetsSpec()));
                var committed=admin.listConsumerGroupOffsets(spec).all().get(2,TimeUnit.SECONDS);
                degraded|=committed.values().stream().anyMatch(offsets->offsets.size()>settings.maxPartitions);
                var wanted=new LinkedHashMap<TopicPartition,OffsetSpec>();committed.values().forEach(offsets->offsets.keySet().stream().limit(settings.maxPartitions).forEach(tp->wanted.put(tp,OffsetSpec.latest())));
                if(wanted.size()>settings.maxPartitions){degraded=true;var retained=new HashSet<>(wanted.keySet().stream().limit(settings.maxPartitions).toList());wanted.keySet().retainAll(retained);}
                if(!wanted.isEmpty()) {
                    var ends=admin.listOffsets(wanted).all().get(2,TimeUnit.SECONDS);
                    for(var offsets:committed.values())for(var offset:offsets.entrySet())if(ends.containsKey(offset.getKey())&&offset.getValue()!=null){maxLag=Math.max(maxLag,Math.max(0,ends.get(offset.getKey()).offset()-offset.getValue().offset()));lagObserved=true;}
                }
            }
            details.put("topics",topics);details.put("partitions",partitions);details.put("offline_partitions",offline);details.put("under_replicated_partitions",under);details.put("consumer_groups",groups.size());
            details.put("lag_status",lagObserved?"OBSERVED":"NOT_CONFIGURED");if(lagObserved)details.put("max_consumer_lag",maxLag);
            degraded|=offline>0||under>0||maxLag>settings.maxLag;
            return of("kafka",capability(),degraded?Status.DEGRADED:Status.HEALTHY,SourceMode.LIVE,"Kafka Admin cluster/config/topic/group/offset APIs",degraded?"Kafka metadata/partition/lag checks need attention":"Broker and configured metadata/partition/lag diagnostics passed","Inspect the observed metadata mode and affected partition/group; no automatic change",degraded?"SEMANTIC_OR_PARTIAL":"",false,details);
        }finally{admin.close(Duration.ofMillis(200));}
    }
}
