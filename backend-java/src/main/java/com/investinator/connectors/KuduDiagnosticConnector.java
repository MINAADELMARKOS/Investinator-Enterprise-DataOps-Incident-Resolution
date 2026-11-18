package com.investinator.connectors;
import com.investinator.health.*;
import static com.investinator.health.HealthCheckResult.*;
import static com.investinator.domain.Json.*;
import org.apache.kudu.client.*;
import com.investinator.health.HealthCheckResult.Status;
import java.util.*;
import java.nio.file.*;
import java.security.cert.CertificateFactory;
/** Official native Kudu client; no session, scanner, table change or rebalance API. */
public class KuduDiagnosticConnector implements DiagnosticConnector {
    private final ConnectorConfig config;
    public KuduDiagnosticConnector(ConnectorConfig config){this.config=config;}
    public String service(){return "kudu";}
    public Descriptor descriptor(){return new Descriptor(service(),"Kudu native RPC","Official 1.18.2 client capability negotiation; exact server release unavailable","Kerberos Subject or JWT; authenticated encrypted RPC required",Set.of(Capability.health,Capability.metadata,Capability.config,Capability.readOnlyChecks),List.of("Master leader discovery does not enumerate every master's availability","No data scans, table writes, rebalancing or automatic failover","Tablet checks require an explicit bounded TABLES allowlist"));}
    protected KuduClient open()throws Exception {
        var client=new KuduClient.KuduClientBuilder(config.value("MASTERS")).defaultAdminOperationTimeoutMs(2000).defaultOperationTimeoutMs(2000).defaultSocketReadTimeoutMs(2000).connectionNegotiationTimeoutMs(2000).workerCount(1).bossCount(1).requireAuthentication(true).encryptionPolicy(AsyncKuduClient.EncryptionPolicy.REQUIRED).build();
        try{if(!config.value("JWT").isBlank())client.jwt(config.value("JWT"));if(!config.value("CA_FILE").isBlank()){try(var input=Files.newInputStream(Path.of(config.value("CA_FILE")))){var certs=CertificateFactory.getInstance("X.509").generateCertificates(input);var values=new ArrayList<org.apache.kudu.shaded.com.google.protobuf.ByteString>();for(var cert:certs)values.add(org.apache.kudu.shaded.com.google.protobuf.ByteString.copyFrom(cert.getEncoded()));client.trustedCertificates(values);}}return client;}catch(Exception e){client.close();throw e;}
    }
    public HealthCheckResult check() {
        if(config.value("MASTERS").isBlank())return ProbeCatalog.absent(service(),"Set CONNECTOR_KUDU_MASTERS and scoped Kerberos/JWT credentials");
        if(config.value("JWT").isBlank()&&System.getProperty("java.security.auth.login.config")==null)return ProbeCatalog.absent(service(),"Configure the backend Kerberos Subject or Kudu JWT; anonymous RPC is refused");
        try(var client=open()) {
            var servers=client.listTabletServers();var leader=client.findLeaderMasterServer();int tablets=0,withoutLeader=0,under=0;
            String tables=config.value("TABLES");var selected=tables.isBlank()?List.<String>of():Arrays.asList(tables.split(","));if(selected.size()>config.limit("MAX_TABLES",3,10))throw new IllegalArgumentException("Table limit");
            for(String table:selected){if(!table.matches("[A-Za-z0-9_.-]{1,200}"))throw new IllegalArgumentException("Table name");var locations=client.openTable(table).getTabletsLocations(2000);if(locations.size()>config.limit("MAX_TABLETS",100,1000))throw new IllegalArgumentException("Tablet response limit");for(var tablet:locations){tablets++;if(tablet.getLeaderReplica()==null)withoutLeader++;if(tablet.getReplicas().size()<config.limit("MIN_REPLICAS",3,9))under++;}}
            boolean bad=servers.getTabletServersCount()==0||leader==null||withoutLeader>0||under>0;
            return of(service(),capability(),bad?Status.DEGRADED:Status.NOT_TESTED,SourceMode.LIVE,"authenticated encrypted Kudu master/table-location RPC",bad?"Kudu master/tablet availability requires attention":"Supported native reads passed; full master/tablet quorum and exact server version remain unverified","Inspect the scoped master/tablet facts; provision explicit master quorum observability",bad?"SEMANTIC_THRESHOLD":"QUORUM_VERSION_NOT_VERIFIED",false,obj("client_version","1.18.2","server_version","NOT_TESTED","tablet_servers",servers.getTabletServersCount(),"leader_master_observed",leader!=null,"tablets_observed",tablets,"tablets_without_leader",withoutLeader,"below_replica_threshold",under,"tablet_sampling",selected.isEmpty()?"NOT_CONFIGURED":"OBSERVED","master_quorum_status","NOT_TESTED"));
        }catch(Exception error){return ProbeCatalog.fail(service(),false,"Authenticated Kudu read failed",error.getClass().getSimpleName());}
    }
}
