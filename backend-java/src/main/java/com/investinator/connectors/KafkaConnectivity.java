package com.investinator.connectors;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import org.apache.kafka.clients.admin.Admin;
import static com.investinator.domain.Json.*;
public final class KafkaConnectivity {
    private KafkaConnectivity() {}
    public static Map<String,Object> health(String brokers) {
        if(brokers==null||brokers.isBlank())return obj("status","not-configured","brokers",0,"topics",List.of(),"mode","read-only");
        Admin client=null;
        try {
            client=Admin.create(Map.of("bootstrap.servers",brokers,"request.timeout.ms",2000,"default.api.timeout.ms",2000));
            var topics=client.listTopics().names().get(2,TimeUnit.SECONDS).stream().sorted().limit(50).toList();
            int count=client.describeCluster().nodes().get(2,TimeUnit.SECONDS).size();
            return obj("status","connected","brokers",count,"topics",topics,"mode","read-only");
        }catch(Exception e){return obj("status","unavailable","brokers",0,"topics",List.of(),"error_type",e.getClass().getSimpleName(),"mode","read-only");}
        finally{if(client!=null)client.close(Duration.ofMillis(200));}
    }
}
