package com.investinator.health;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.AuthorizationException;
import org.springframework.core.env.StandardEnvironment;
/** Explicit negative authorization test against a random owned local cluster; never a model tool. */
public final class KafkaReadOnlyVerifier {
    public static void main(String[] args)throws Exception {
        if(args.length!=2||!args[0].equals("--owned-local-fixture"))throw new IllegalArgumentException("Owned marker required");
        var props=KafkaHealthProbe.configuration(new StandardEnvironment());if(!props.get("bootstrap.servers").toString().matches("(127\\.0\\.0\\.1|localhost):[0-9]{1,5}"))throw new IllegalArgumentException("Local fixture required");
        try(Admin admin=Admin.create(props)) {
            if(!args[1].equals(admin.describeCluster().clusterId().get(3,TimeUnit.SECONDS)))throw new IllegalArgumentException("Wrong cluster identity");
            var partition=new org.apache.kafka.common.TopicPartition("orders.events",0);long end=admin.listOffsets(Map.of(partition,OffsetSpec.latest())).all().get(3,TimeUnit.SECONDS).get(partition).offset();var committed=admin.listConsumerGroupOffsets("investinator-demo").partitionsToOffsetAndMetadata().get(3,TimeUnit.SECONDS).get(partition);if(end<5||committed==null||committed.offset()<5)throw new IllegalStateException("Expected persisted fixture offsets are absent");System.out.println("DATA: "+end+":"+committed.offset());
            try{admin.createTopics(List.of(new NewTopic("investinator-denied-probe",1,(short)1))).all().get(3,TimeUnit.SECONDS);throw new IllegalStateException("Diagnostic principal unexpectedly created a topic");}catch(ExecutionException e){if(!(e.getCause() instanceof AuthorizationException))throw e;}
        }
        props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");props.put("max.block.ms",2000);props.put("delivery.timeout.ms",3000);props.put("request.timeout.ms",2000);
        try(var producer=new KafkaProducer<String,String>(props)) {
            try{producer.send(new ProducerRecord<>("orders.events","denied","SIMULATED-negative-authorization-test")).get(4,TimeUnit.SECONDS);throw new IllegalStateException("Diagnostic principal unexpectedly wrote a record");}catch(ExecutionException e){if(!(e.getCause() instanceof AuthorizationException))throw e;}
        }
        System.out.println("PASS: diagnostic principal can inspect but cannot create topics or produce records");
    }
}
