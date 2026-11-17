package com.investinator.connectors;
import com.investinator.health.*;
import static com.investinator.health.HealthCheckResult.*;
import java.util.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.*;
import org.apache.zookeeper.*;
/** Legacy-only, explicit isolated fixture. No create/setData/delete/setACL/reconfig operation. */
public class LegacyZooKeeperConnector implements DiagnosticConnector {
    @FunctionalInterface public interface ModeEvidence {HealthCheckResult read()throws Exception;}
    private final ConnectorConfig config;private final ModeEvidence kafka;
    public LegacyZooKeeperConnector(ConnectorConfig config,ModeEvidence kafka){this.config=config;this.kafka=kafka;}
    public String service(){return "zookeeper";}
    public Descriptor descriptor(){return new Descriptor(service(),"ZooKeeper native digest read + read-only srvr","ZooKeeper 3.5–3.9 isolated legacy fixture; verified Kafka legacy config required","Digest ACL on fixed fixture znode; loopback only",Set.of(Capability.health,Capability.metadata,Capability.config,Capability.readOnlyChecks),List.of("Never contacted for KRaft or unknown Kafka mode","No production ZooKeeper endpoint; this adapter is intentionally isolated","Server must explicitly allow read-only srvr command"));}
    public HealthCheckResult check() {
        if(!config.configured())return ProbeCatalog.absent(service(),"Configure explicit isolated legacy ZooKeeper fixture; never required by KRaft");
        try {
            var uri=URI.create(config.value("URL"));if(!"zk".equals(uri.getScheme())||!Set.of("127.0.0.1","localhost","::1").contains(uri.getHost())||!config.flag("LOCAL_FIXTURE")||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("ZooKeeper only supports isolated local legacy fixture");
            var mode=kafka.read();if(!"LEGACY_ZOOKEEPER".equals(mode.details().get("metadata_mode")))return of(service(),capability(),Status.NOT_TESTED,SourceMode.NOT_TESTED,"verified Kafka configuration guard","Kafka is KRaft or its legacy mode is unverified; ZooKeeper was not contacted","Use only a separate verified legacy fixture","LEGACY_MODE_REQUIRED",false,Map.of());
            if(config.value("USER").isBlank()||config.value("PASSWORD").isBlank())return ProbeCatalog.absent(service(),"Configure scoped digest fixture credentials");
            int port=uri.getPort()<0?2181:uri.getPort();String version;
            try(var socket=new Socket()){socket.connect(new InetSocketAddress(uri.getHost(),port),1000);socket.setSoTimeout(2000);socket.getOutputStream().write("srvr".getBytes(java.nio.charset.StandardCharsets.US_ASCII));version=new String(socket.getInputStream().readNBytes(8192),java.nio.charset.StandardCharsets.UTF_8);}
            if(!version.matches("(?s).*Zookeeper version: 3\\.[5-9]\\..*"))return ProbeCatalog.fail(service(),false,"Unsupported or hidden ZooKeeper server version","UNSUPPORTED_VERSION");
            var ready=new CountDownLatch(1);try(var client=new ZooKeeper(uri.getHost()+":"+port,3000,event->{if(event.getState()==Watcher.Event.KeeperState.SyncConnected)ready.countDown();})) {
                if(!ready.await(2,TimeUnit.SECONDS))throw new TimeoutException();client.addAuthInfo("digest",(config.value("USER")+":"+config.value("PASSWORD")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var stat=new org.apache.zookeeper.data.Stat();byte[] value=client.getData("/investinator-health",false,stat);if(!Arrays.equals(value,"SIMULATED".getBytes(java.nio.charset.StandardCharsets.UTF_8)))return ProbeCatalog.fail(service(),false,"Fixture znode contract failed","CONTRACT_MISMATCH");
                return of(service(),capability(),Status.HEALTHY,mode.source_mode()==SourceMode.SIMULATED?SourceMode.SIMULATED:SourceMode.LIVE,"read-only ZooKeeper srvr plus digest-ACL getData in isolated legacy fixture","Verified legacy mode, supported server and authenticated fixture read passed","No action required; never attach this dependency to KRaft","",false,Map.of("metadata_mode","LEGACY_ZOOKEEPER","data_source_mode","SIMULATED","server_version",version.lines().filter(l->l.startsWith("Zookeeper version:")).findFirst().orElse("UNKNOWN"),"znode_version",stat.getVersion()));
            }
        }catch(Exception e){return ProbeCatalog.fail(service(),false,"Isolated legacy ZooKeeper read failed",e.getClass().getSimpleName());}
    }
}
