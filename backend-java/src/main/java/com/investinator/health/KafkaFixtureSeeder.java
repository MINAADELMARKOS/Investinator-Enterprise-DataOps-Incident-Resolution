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
        if(args.length!=2||!args[0].equals("--owned-local-fixture"))throw new IllegalArgumentException("Owned local fixture marker and expected cluster ID are required");
        var env=new StandardEnvironment();var config=KafkaHealthProbe.configuration(env);
        String brokers=config.get("bootstrap.servers").toString();
        if(!brokers.matches("(127\\.0\\.0\\.1|localhost):[0-9]{1,5}"))throw new IllegalArgumentException("Fixture seeding requires a local listener");
        try(Admin admin=Admin.create(config)) {
            if(!args[1].equals(admin.describeCluster().clusterId().get(3,TimeUnit.SECONDS)))throw new IllegalArgumentException("Cluster identity does not match owned lab");
            if(!admin.listTopics().names().get(3,TimeUnit.SECONDS).contains("orders.events"))admin.createTopics(List.of(new NewTopic("orders.events",1,(short)1))).all().get(5,TimeUnit.SECONDS);
        }
        var producerConfig=new HashMap<>(config);producerConfig.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");producerConfig.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        try(var producer=new KafkaProducer<String,String>(producerConfig)) {for(int i=0;i<5;i++)producer.send(new ProducerRecord<>("orders.events","fixture-"+i,"{\"source_mode\":\"SIMULATED\",\"order\":"+i+"}")).get(3,TimeUnit.SECONDS);}
        var consumerConfig=new HashMap<>(config);consumerConfig.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");consumerConfig.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");consumerConfig.put("group.id","investinator-demo");consumerConfig.put("auto.offset.reset","earliest");consumerConfig.put("enable.auto.commit",false);
        try(var consumer=new KafkaConsumer<String,String>(consumerConfig)) {
            consumer.subscribe(List.of("orders.events"));long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);int count=0;
            while(System.nanoTime()<end&&count<5)count+=consumer.poll(Duration.ofMillis(500)).count();
            if(count<5)throw new IllegalStateException("Local fixture consumer did not observe expected records");consumer.commitSync(Duration.ofSeconds(3));
        }
        System.out.println("PASS: owned local Kafka fixture produced/consumed SIMULATED orders with committed offsets");
    }
}
