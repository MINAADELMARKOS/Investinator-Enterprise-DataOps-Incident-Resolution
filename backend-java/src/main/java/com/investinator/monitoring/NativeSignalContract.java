package com.investinator.monitoring;

import java.time.*;
import java.util.*;
import com.investinator.api.ApiFailure;

/** Fixed read-only adapter handoff contract. No native ingestion endpoint or certification bypass. */
public final class NativeSignalContract {
    private NativeSignalContract() {}
    public record Binding(String tenant,String cluster,String service,String version,String mode,
                          Set<String> capabilities) {
        public Binding {
            for(String value:List.of(tenant,cluster,service,version,mode))
                if(!value.matches("[A-Za-z0-9_.:-]{1,100}"))throw new IllegalArgumentException("Invalid trusted binding");
            capabilities=Set.copyOf(capabilities);
            if(capabilities.isEmpty()||capabilities.size()>30)throw new IllegalArgumentException("Missing bounded capabilities");
        }
    }
    public record Sample(String tenant,String cluster,String service,String version,String mode,
                         String observed_at,String evidence_id,String source_mode,
                         Map<String,Double> measurements,Set<String> capabilities) {
        public Sample {
            Instant.parse(observed_at);measurements=Map.copyOf(measurements);capabilities=Set.copyOf(capabilities);
            if(evidence_id==null||!evidence_id.matches("[A-Za-z0-9_.:-]{1,150}")||measurements.size()>30||capabilities.size()>30||measurements.values().stream().anyMatch(v->!Double.isFinite(v)))throw new IllegalArgumentException("Invalid bounded sample");
        }
    }
    public record Accepted(Sample sample,String ingested_at,String freshness,String provenance) {}
    public static String status(boolean requested){return requested?"NOT_CERTIFIED":"DISABLED";}
    /** Only protocol-fixture data can exercise normalization until independently certified ingress exists. */
    public static Accepted normalizeFixture(Binding trusted,Sample sample,Clock clock) {
        if(!Objects.equals(trusted.tenant(),sample.tenant())||!Objects.equals(trusted.cluster(),sample.cluster())||!Objects.equals(trusted.service(),sample.service()))throw new ApiFailure(403,"Foreign or anonymous source binding");
        if(!"SIMULATED".equals(sample.source_mode()))throw new ApiFailure(403,"Native LIVE ingestion is NOT_CERTIFIED");
        if(!trusted.version().equals(sample.version())||!trusted.mode().equals(sample.mode())||!trusted.capabilities().containsAll(sample.capabilities())||sample.capabilities().isEmpty())throw new ApiFailure(422,"Unverified version, mode or capability");
        boolean kafka=trusted.service().equals("kafka"),zk=trusted.service().equals("zookeeper");
        if(kafka&&(!Set.of("KRAFT","LEGACY_ZOOKEEPER").contains(trusted.mode())||trusted.version().startsWith("4.")&&!trusted.mode().equals("KRAFT"))||zk&&!trusted.mode().equals("LEGACY_ZOOKEEPER")||trusted.mode().equals("KRAFT")&&sample.capabilities().stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains("zookeeper")))throw new ApiFailure(422,"Incompatible coordination capability");
        Instant now=clock.instant(),observed=Instant.parse(sample.observed_at());
        if(observed.isAfter(now.plusSeconds(30)))throw new ApiFailure(422,"Source clock skew exceeds bound");
        String freshness=observed.isBefore(now.minusSeconds(60))?"STALE":"FRESH";
        return new Accepted(sample,now.toString(),freshness,"SIMULATED_PROTOCOL_FIXTURE_NOT_ENTERPRISE_CERTIFICATION");
    }
}
