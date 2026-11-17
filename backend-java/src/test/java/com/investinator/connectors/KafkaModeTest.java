package com.investinator.connectors;
import com.investinator.health.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.env.MockEnvironment;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.*;
import org.apache.kafka.common.config.ConfigResource;
import java.util.*;
class KafkaModeTest {
    @Test void brokerConfigReliablySeparatesKraftFromLegacyAndUnknown()throws Exception {
        for(String mode:List.of("KRAFT","LEGACY_ZOOKEEPER","UNKNOWN")){
            var admin=mock(Admin.class);var cluster=mock(DescribeClusterResult.class);when(admin.describeCluster()).thenReturn(cluster);when(cluster.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(new Node(1,"fixture",9092))));when(cluster.clusterId()).thenReturn(KafkaFuture.completedFuture("fixture-cluster"));
            var configs=mock(DescribeConfigsResult.class);when(admin.describeConfigs(anyCollection())).thenReturn(configs);var resource=new ConfigResource(ConfigResource.Type.BROKER,"1");when(configs.all()).thenReturn(KafkaFuture.completedFuture(Map.of(resource,new Config(List.of(new ConfigEntry("process.roles",mode.equals("KRAFT")?"broker,controller":""),new ConfigEntry("zookeeper.connect",mode.equals("LEGACY_ZOOKEEPER")?"legacy-fixture:2181":null))))));
            var topics=mock(ListTopicsResult.class);when(admin.listTopics()).thenReturn(topics);when(topics.names()).thenReturn(KafkaFuture.completedFuture(Set.of()));var groups=mock(ListConsumerGroupsResult.class);when(admin.listConsumerGroups()).thenReturn(groups);when(groups.valid()).thenReturn(KafkaFuture.completedFuture(List.of()));
            var quorumResult=mock(DescribeMetadataQuorumResult.class);var quorum=mock(QuorumInfo.class);when(admin.describeMetadataQuorum()).thenReturn(quorumResult);when(quorumResult.quorumInfo()).thenReturn(KafkaFuture.completedFuture(quorum));when(quorum.leaderId()).thenReturn(1);when(quorum.voters()).thenReturn(List.of(mock(QuorumInfo.ReplicaState.class)));
            var env=new MockEnvironment().withProperty("KAFKA_BOOTSTRAP_SERVERS","fixture:9092");var probe=new KafkaHealthProbe(env,new HealthSettings(env)){protected Admin openAdmin(Map<String,Object> p){return admin;}};
            var result=probe.check();assertEquals(mode,result.details().get("metadata_mode"));if(mode.equals("KRAFT"))verify(admin).describeMetadataQuorum();else verify(admin,never()).describeMetadataQuorum();
            assertTrue(mockingDetails(admin).getInvocations().stream().noneMatch(i->i.getMethod().getName().matches("create.*|delete.*|alter.*|incrementalAlter.*")));
        }
    }
}
