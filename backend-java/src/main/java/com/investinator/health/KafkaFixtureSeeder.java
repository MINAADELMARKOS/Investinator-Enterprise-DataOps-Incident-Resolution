package com.investinator.health;
import java.util.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.springframework.core.env.StandardEnvironment;
/** Explicit local-lab setup only. Never registered as a diagnostic probe or model tool. */
public final class KafkaFixtureSeeder {
    public static void main(String[] args) throws Exception {
        if((args.length!=2&&args.length!=3)||!args[0].equals("--owned-local-fixture"))throw new IllegalArgumentException("Owned local fixture marker and expected cluster ID are required");
        var env=new StandardEnvironment();var config=KafkaHealthProbe.configuration(env);
        String brokers=config.get("bootstrap.servers").toString();
        if(!brokers.matches("(127\\.0\\.0\\.1|localhost):[0-9]{1,5}"))throw new IllegalArgumentException("Fixture seeding requires a local listener");
        long existingRecords=0;
        try(Admin admin=Admin.create(config)) {
            if(!args[1].equals(admin.describeCluster().clusterId().get(3,TimeUnit.SECONDS)))throw new IllegalArgumentException("Cluster identity does not match owned lab");
            var permissions=new ArrayList<org.apache.kafka.common.acl.AclBinding>();
            for(var operation:List.of(org.apache.kafka.common.acl.AclOperation.DESCRIBE,org.apache.kafka.common.acl.AclOperation.DESCRIBE_CONFIGS))permissions.add(new org.apache.kafka.common.acl.AclBinding(new org.apache.kafka.common.resource.ResourcePattern(org.apache.kafka.common.resource.ResourceType.CLUSTER,"kafka-cluster",org.apache.kafka.common.resource.PatternType.LITERAL),new org.apache.kafka.common.acl.AccessControlEntry("User:health","*",operation,org.apache.kafka.common.acl.AclPermissionType.ALLOW)));
            for(var resource:List.of(org.apache.kafka.common.resource.ResourceType.TOPIC,org.apache.kafka.common.resource.ResourceType.GROUP))permissions.add(new org.apache.kafka.common.acl.AclBinding(new org.apache.kafka.common.resource.ResourcePattern(resource,"*",org.apache.kafka.common.resource.PatternType.LITERAL),new org.apache.kafka.common.acl.AccessControlEntry("User:health","*",org.apache.kafka.common.acl.AclOperation.DESCRIBE,org.apache.kafka.common.acl.AclPermissionType.ALLOW)));
            admin.createAcls(permissions).all().get(5,TimeUnit.SECONDS);
            if(args.length==3&&!args[2].equals("--access-only"))throw new IllegalArgumentException("Unknown fixture option");
            var partition=new org.apache.kafka.common.TopicPartition("orders.events",0);
            if(!admin.listTopics().names().get(3,TimeUnit.SECONDS).contains("orders.events"))admin.createTopics(List.of(new NewTopic("orders.events",1,(short)1))).all().get(5,TimeUnit.SECONDS);
            else existingRecords=admin.listOffsets(Map.of(partition,OffsetSpec.latest())).all().get(3,TimeUnit.SECONDS).get(partition).offset();
            if(existingRecords>=5){var offsets=admin.listConsumerGroupOffsets("investinator-demo").partitionsToOffsetAndMetadata().get(3,TimeUnit.SECONDS);if(offsets.containsKey(partition)&&offsets.get(partition)!=null&&offsets.get(partition).offset()>=5){System.out.println("PASS: diagnostic ACLs and existing owned Kafka fixture verified");return;}}
        }
        var producerConfig=new HashMap<>(config);producerConfig.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");producerConfig.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        try(var producer=new KafkaProducer<String,String>(producerConfig)) {for(int i=(int)Math.min(5,existingRecords);i<5;i++)producer.send(new ProducerRecord<>("orders.events","fixture-"+i,"{\"source_mode\":\"SIMULATED\",\"order\":"+i+"}")).get(3,TimeUnit.SECONDS);}
        var consumerConfig=new HashMap<>(config);consumerConfig.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");consumerConfig.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");consumerConfig.put("group.id","investinator-demo");consumerConfig.put("auto.offset.reset","earliest");consumerConfig.put("enable.auto.commit",false);
        try(var consumer=new KafkaConsumer<String,String>(consumerConfig)) {
            var partitions=List.of(new org.apache.kafka.common.TopicPartition("orders.events",0));consumer.assign(partitions);consumer.seekToBeginning(partitions);long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);int count=0;
            while(System.nanoTime()<end&&count<5)count+=consumer.poll(Duration.ofMillis(500)).count();
            if(count<5)throw new IllegalStateException("Local fixture consumer did not observe expected records");consumer.commitSync(Duration.ofSeconds(3));
        }
        System.out.println("PASS: owned local Kafka fixture produced/consumed SIMULATED orders with committed offsets");
    }
}
