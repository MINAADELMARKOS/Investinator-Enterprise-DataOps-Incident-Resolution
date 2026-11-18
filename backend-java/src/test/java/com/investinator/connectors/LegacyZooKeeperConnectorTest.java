package com.investinator.connectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import com.investinator.health.*;
import static com.investinator.health.HealthCheckResult.*;
import org.springframework.mock.env.MockEnvironment;
import org.apache.zookeeper.*;
import org.apache.zookeeper.server.*;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.apache.zookeeper.data.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
class LegacyZooKeeperConnectorTest {
    @TempDir Path directory;
    HealthCheckResult mode(String mode){return of("kafka","mode",Status.HEALTHY,SourceMode.SIMULATED,"test legacy broker configuration fixture","Fixture","None","",false,Map.of("metadata_mode",mode));}
    @Test void kraftNeverContactsZooKeeperEvenIfEndpointIsConfigured(){
        var env=new MockEnvironment().withProperty("CONNECTOR_ZOOKEEPER_URL","zk://127.0.0.1:1").withProperty("CONNECTOR_ZOOKEEPER_LOCAL_FIXTURE","true");
        var report=new LegacyZooKeeperConnector(new ConnectorConfig(env,"zookeeper"),()->mode("KRAFT")).check();assertEquals(Status.NOT_TESTED,report.status());assertEquals("LEGACY_MODE_REQUIRED",report.error_type());
    }
    @Test void actualIsolatedServerRequiresDigestAclAndDiagnosisNeverChangesZnode()throws Exception {
        System.setProperty("zookeeper.4lw.commands.whitelist","srvr");System.setProperty("zookeeper.admin.enableServer","false");
        var server=new ZooKeeperServer(directory.toFile(),directory.toFile(),3000);var factory=new NIOServerCnxnFactory();factory.configure(new InetSocketAddress("127.0.0.1",0),10);factory.startup(server);
        try {
            String endpoint="127.0.0.1:"+factory.getLocalPort();var ready=new CountDownLatch(1);
            try(var setup=new ZooKeeper(endpoint,3000,e->{if(e.getState()==Watcher.Event.KeeperState.SyncConnected)ready.countDown();})){
                assertTrue(ready.await(2,TimeUnit.SECONDS));setup.addAuthInfo("digest","reader:fixture-only".getBytes());setup.create("/investinator-health","SIMULATED".getBytes(),new ArrayList<>(List.of(new ACL(ZooDefs.Perms.READ,new Id("digest",DigestAuthenticationProvider.generateDigest("reader:fixture-only"))))),CreateMode.PERSISTENT);
                int count=server.getZKDatabase().getDataTree().getNodeCount();
                var env=new MockEnvironment().withProperty("CONNECTOR_ZOOKEEPER_URL","zk://"+endpoint).withProperty("CONNECTOR_ZOOKEEPER_LOCAL_FIXTURE","true").withProperty("CONNECTOR_ZOOKEEPER_USER","reader").withProperty("CONNECTOR_ZOOKEEPER_PASSWORD","fixture-only");
                var connector=new LegacyZooKeeperConnector(new ConnectorConfig(env,"zookeeper"),()->mode("LEGACY_ZOOKEEPER"));var report=connector.check();assertEquals(Status.HEALTHY,report.status(),report.error_type());assertEquals(SourceMode.SIMULATED,report.source_mode());assertTrue(report.details().get("server_version").toString().contains("3.9.7"));
                env.setProperty("CONNECTOR_ZOOKEEPER_PASSWORD","wrong-secret-do-not-echo");var denied=connector.check();assertEquals(Status.UNHEALTHY,denied.status());assertFalse(denied.toString().contains("wrong-secret"));
                var stat=new Stat();assertEquals("SIMULATED",new String(setup.getData("/investinator-health",false,stat)));assertEquals(0,stat.getVersion());assertEquals(count,server.getZKDatabase().getDataTree().getNodeCount());
            }
        }finally{factory.shutdown();server.shutdown();System.clearProperty("zookeeper.4lw.commands.whitelist");System.clearProperty("zookeeper.admin.enableServer");}
    }
}
