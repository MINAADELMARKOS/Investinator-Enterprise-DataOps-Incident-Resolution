package com.investinator.connectors;
import com.investinator.health.*;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import java.util.*;
@Component
public class ConnectorRegistry implements DiagnosticProviders {
    private final Map<String,DiagnosticConnector> connectors;private final Set<String> allowed;
    public ConnectorRegistry(Environment env,HealthSettings settings) {
        var catalog=new LinkedHashMap<String,DiagnosticConnector>();
        for(String service:List.of("spark","yarn","hdfs","nifi","airflow","impala","cloudera-manager","atlas","ranger"))catalog.put(service,new RestDiagnosticConnector(new ConnectorConfig(env,service)));
        var kafka=new KafkaHealthProbe(env,settings);
        catalog.put("kafka",new DiagnosticConnector(){public String service(){return "kafka";}public Descriptor descriptor(){return new Descriptor(service(),"Kafka Admin native RPC","KRaft via process.roles/quorum API; legacy via zookeeper.connect only","SASL/GSSAPI/SCRAM/PLAIN + TLS configured through backend",Set.of(Capability.health,Capability.metrics,Capability.metadata,Capability.config,Capability.readOnlyChecks),List.of("Kafka Admin exposes negotiated capability evidence, not a reliable exact broker release","Bounded topic/group samples; missing lag is NOT_CONFIGURED","No Kafka producer, consumer, config mutation or ACL mutation in diagnostics"));}public HealthCheckResult check()throws Exception{return kafka.check();}});
        catalog.put("hive",new HiveServer2Connector(new ConnectorConfig(env,"hive")));
        catalog.put("hive-metastore",new HiveMetastoreConnector(new ConnectorConfig(env,"hive-metastore")));
        catalog.put("kudu",new KuduDiagnosticConnector(new ConnectorConfig(env,"kudu")));
        catalog.put("zookeeper",new LegacyZooKeeperConnector(new ConnectorConfig(env,"zookeeper"),kafka::check));
        connectors=Collections.unmodifiableMap(catalog);String scopes=env.getProperty("CONNECTOR_ALLOWED_SERVICES",String.join(",",catalog.keySet()));allowed=Set.copyOf(Arrays.asList(scopes.split(",")));
    }
    public List<HealthProbe> probes(){return new ArrayList<>(connectors.values());}
    public List<DiagnosticConnector.Descriptor> descriptors(){return connectors.values().stream().filter(c->allowed.contains(c.service())).map(DiagnosticConnector::descriptor).toList();}
    public DiagnosticConnector find(String service){if(!allowed.contains(service))throw new SecurityException("Service outside diagnostics scope");var value=connectors.get(service);if(value==null)throw new NoSuchElementException();return value;}
}
