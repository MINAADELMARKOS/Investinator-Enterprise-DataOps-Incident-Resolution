package com.investinator.monitoring;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
class NativeSignalContractTest {
    final Clock clock=Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"),ZoneOffset.UTC);
    NativeSignalContract.Binding binding(String mode,String version){return new NativeSignalContract.Binding("demo","cluster","kafka",version,mode,Set.of("health","metadata","zookeeper"));}
    NativeSignalContract.Sample sample(String tenant,String cluster,String version,String mode,String source,String time,Set<String> caps){return new NativeSignalContract.Sample(tenant,cluster,"kafka",version,mode,time,"EV-fixture",source,Map.of("lag",1.0),caps);}
    @Test void disabledEvenWhenRequestedUntilIndependentCertification(){assertEquals("DISABLED",NativeSignalContract.status(false));assertEquals("NOT_CERTIFIED",NativeSignalContract.status(true));}
    @Test void fixturePreservesEventTimeAndServerIngestionFreshness(){var s=sample("demo","cluster","4.3","KRAFT","SIMULATED",clock.instant().minusSeconds(120).toString(),Set.of("health"));var accepted=NativeSignalContract.normalizeFixture(binding("KRAFT","4.3"),s,clock);assertEquals(s,accepted.sample());assertEquals(clock.instant().toString(),accepted.ingested_at());assertEquals("STALE",accepted.freshness());assertTrue(accepted.provenance().startsWith("SIMULATED"));}
    @Test void anonymousForeignAndAssertedLiveDenied(){for(var s:List.of(sample("other","cluster","4.3","KRAFT","SIMULATED",clock.instant().toString(),Set.of("health")),sample("demo","foreign","4.3","KRAFT","SIMULATED",clock.instant().toString(),Set.of("health")),sample("demo","cluster","4.3","KRAFT","LIVE",clock.instant().toString(),Set.of("health"))))assertThrows(Exception.class,()->NativeSignalContract.normalizeFixture(binding("KRAFT","4.3"),s,clock));}
    @Test void versionCapabilityClockAndKraftZookeeperMismatchDenied(){for(var s:List.of(sample("demo","cluster","3.9","KRAFT","SIMULATED",clock.instant().toString(),Set.of("health")),sample("demo","cluster","4.3","KRAFT","SIMULATED",clock.instant().plusSeconds(31).toString(),Set.of("health")),sample("demo","cluster","4.3","KRAFT","SIMULATED",clock.instant().toString(),Set.of("zookeeper")),sample("demo","cluster","4.3","KRAFT","SIMULATED",clock.instant().toString(),Set.of("write"))))assertThrows(Exception.class,()->NativeSignalContract.normalizeFixture(binding("KRAFT","4.3"),s,clock));}
    @Test void onlyCompatibleLegacyModeAllowed(){assertThrows(Exception.class,()->NativeSignalContract.normalizeFixture(binding("LEGACY_ZOOKEEPER","4.3"),sample("demo","cluster","4.3","LEGACY_ZOOKEEPER","SIMULATED",clock.instant().toString(),Set.of("health")),clock));assertEquals("FRESH",NativeSignalContract.normalizeFixture(binding("LEGACY_ZOOKEEPER","3.9"),sample("demo","cluster","3.9","LEGACY_ZOOKEEPER","SIMULATED",clock.instant().toString(),Set.of("health")),clock).freshness());}
}
