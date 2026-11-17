package com.investinator.evidence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import com.investinator.domain.Json;
class NormalizedEvidenceTest {
    private NormalizedEvidence event(String tenant,String source,String message,Instant time,Instant arrival) {
        return NormalizedEvidence.normalize(tenant,source,new NormalizedEvidence.Service("kraft-controller","fixture"),message,time,arrival,null,null);
    }
    @Test void stableSourceTimeAndTenantDeduplicateWithoutConflatingTenants() {
        var time=Instant.parse("2026-11-17T00:00:00Z");var one=event("demo","fixture://source/1","message",time,time.plusSeconds(1));
        var retry=event("demo","fixture://source/1","same source retry",time,time.plusSeconds(5));
        assertEquals(one.evidence_id(),retry.evidence_id());
        assertNotEquals(one.evidence_id(),event("other","fixture://source/1","message",time,time).evidence_id());
        assertNotEquals(one.evidence_id(),event("demo","fixture://source/1","message",time.plusSeconds(1),time).evidence_id());
        assertTrue(one.validFor("demo",one.evidence_id()));assertFalse(one.validFor("other",one.evidence_id()));assertFalse(one.validFor("demo","forged-id"));
    }
    @Test void redactsBeforeIndexingAndPreservesEventTimeWithLagAndClockSkewFlags() {
        var time=Instant.parse("2026-11-17T00:00:00Z");var redacted=event("demo","fixture://source/1","password=unsafe jane@example.com",time,time.plusSeconds(1000));
        assertEquals("[REDACTED] [REDACTED]",redacted.event().original());assertEquals("REDACTED",redacted.sensitivity());
        assertEquals(1000000,redacted.ingestion_lag_ms());assertTrue(redacted.quality_flags().contains("LATE_ARRIVAL"));
        var future=event("demo","fixture://source/1","message",time.plusSeconds(600),time);
        assertEquals(0,future.ingestion_lag_ms());assertTrue(future.quality_flags().contains("FUTURE_CLOCK_SKEW"));
        assertEquals(time.toString(),redacted.timestamp());
        var decoded=Json.decode(Json.encode(redacted),NormalizedEvidence.class);assertEquals(redacted,decoded);
        assertTrue(Json.encode(redacted).contains("\"@timestamp\""));
    }
    @Test void boundsPayloadsAndRejectsInvalidIdentity() {
        var time=Instant.now();
        assertThrows(IllegalArgumentException.class,()->event("","fixture://s","message",time,time));
        assertThrows(IllegalArgumentException.class,()->event("demo","","message",time,time));
        assertThrows(IllegalArgumentException.class,()->event("demo","fixture://s","x".repeat(65537),time,time));
        var truncated=event("demo","fixture://s","x".repeat(9000),time,time);
        assertEquals(8192,truncated.event().original().length());assertTrue(truncated.quality_flags().contains("TRUNCATED"));
    }
}
