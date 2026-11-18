package com.investinator.evidence;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import com.investinator.domain.Json;
public final class EvidenceFixtureGenerator {
    private EvidenceFixtureGenerator() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Supply a local output directory");
        Path directory=Path.of(args[0]);Files.createDirectories(directory);
        Instant now=Instant.now();String trace="0123456789abcdef0123456789abcdef";
        var events=List.of(
            NormalizedEvidence.normalize("demo","fixture://orders/network-1",new NormalizedEvidence.Service("network-link","fixture-v1"),"upstream link packet loss token=fixture-secret contact sre@example.com",now.minusSeconds(12),now,trace,"1111111111111111"),
            NormalizedEvidence.normalize("demo","fixture://orders/controller-1",new NormalizedEvidence.Service("kraft-controller","fixture-v1"),"controller heartbeat expired after upstream link instability",now.minusSeconds(10),now,trace,"2222222222222222"),
            NormalizedEvidence.normalize("demo","fixture://orders/broker-1",new NormalizedEvidence.Service("kafka-broker","fixture-v1"),"broker timeout, downstream consumer lag rising",now.minusSeconds(8),now,trace,"3333333333333333"),
            NormalizedEvidence.normalize("other","fixture://orders/controller-foreign",new NormalizedEvidence.Service("kraft-controller","fixture-v1"),"controller heartbeat expired FOREIGN_TENANT_RECORD",now.minusSeconds(9),now,trace,"4444444444444444"));
        String content=String.join("\n",events.stream().map(Json::encode).toList())+"\n";
        Files.writeString(directory.resolve("evidence.ndjson"),content,StandardOpenOption.CREATE_NEW);
        // Repeat exact identities through a second file to prove index-level deduplication.
        Files.writeString(directory.resolve("duplicates.ndjson"),content,StandardOpenOption.CREATE_NEW);
        System.out.println("Wrote four SIMULATED normalized events and exact duplicates to a new fixture directory");
    }
}
