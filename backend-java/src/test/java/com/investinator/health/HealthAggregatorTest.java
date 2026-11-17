package com.investinator.health;
import static org.junit.jupiter.api.Assertions.*;
import static com.investinator.health.HealthCheckResult.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
class HealthAggregatorTest {
    private HealthProbe probe(String name,java.util.concurrent.Callable<HealthCheckResult> action){return new HealthProbe(){public String service(){return name;}public String capability(){return "test";}public HealthCheckResult check() throws Exception{return action.call();}};}
    private HealthCheckResult good(String name){return ProbeCatalog.ok(name,"test","Safe evidence",false,Map.of());}
    private HealthSettings settings(String... pairs){var env=new MockEnvironment();for(int i=0;i<pairs.length;i+=2)env.setProperty("investinator.health."+pairs[i],pairs[i+1]);return new HealthSettings(env);}
    @Test void timeoutReturnsPartialWithoutLosingSuccessfulChecks() {
        var slow=probe("slow",()->{Thread.sleep(5000);return good("slow");});
        try(varRunner r=new varRunner(new HealthAggregator(List.of(probe("fast",()->good("fast")),slow),settings("timeout-ms","60","deadline-ms","150")))) {
            long before=System.nanoTime();var report=r.value.refresh();
            assertTrue((System.nanoTime()-before)/1_000_000<1500);assertTrue(report.partial());assertFalse(report.success());
            assertEquals(Status.HEALTHY,report.checks().getFirst().status());assertEquals("TIMEOUT",report.checks().getLast().error_type());
        }
    }
    @Test void retriesTransientFailuresButNeverEchoesExceptionSecrets() {
        var calls=new AtomicInteger();var p=probe("retry",()->{if(calls.incrementAndGet()==1)throw new java.io.IOException("secret=NEVER-EXPOSE");return good("retry");});
        try(varRunner r=new varRunner(new HealthAggregator(List.of(p),settings()))) {
            var report=r.value.refresh();assertTrue(report.success());assertEquals(2,calls.get());assertEquals(2,report.checks().getFirst().details().get("attempts"));assertFalse(report.toString().contains("NEVER-EXPOSE"));
        }
    }
    @Test void persistentFailureOpensCircuitAndRecoversAfterCooldown() throws Exception {
        var calls=new AtomicInteger();var p=probe("broken",()->{calls.incrementAndGet();return ProbeCatalog.fail("broken",false,"Failed safely","AUTHORIZATION");});
        try(varRunner r=new varRunner(new HealthAggregator(List.of(p),settings("failure-threshold","1","cooldown-ms","30")))) {
            assertFalse(r.value.refresh().success());assertEquals("CIRCUIT_OPEN",r.value.refresh().checks().getFirst().error_type());assertEquals(1,calls.get());Thread.sleep(45);r.value.refresh();assertEquals(2,calls.get());
        }
    }
    @Test void boundedParallelismAndQueueSaturationAreTruthful() {
        var running=new AtomicInteger();var max=new AtomicInteger();var list=new ArrayList<HealthProbe>();
        for(int i=0;i<12;i++){String name="p"+i;list.add(probe(name,()->{int n=running.incrementAndGet();max.accumulateAndGet(n,Math::max);try{Thread.sleep(70);return good(name);}finally{running.decrementAndGet();}}));}
        try(varRunner r=new varRunner(new HealthAggregator(list,settings("parallelism","2","queue-size","2","timeout-ms","500")))) {
            var report=r.value.refresh();assertTrue(max.get()<=2);assertTrue(report.partial());assertTrue(report.checks().stream().anyMatch(c->"SATURATED".equals(c.error_type())));
        }
    }
    @Test void cacheDoesNotIssueNewCallsAndOptionalNotConfiguredIsExplicit() {
        var calls=new AtomicInteger();var p=probe("optional",()->{calls.incrementAndGet();return ProbeCatalog.absent("optional","Configure credentials");});
        try(varRunner r=new varRunner(new HealthAggregator(List.of(p),settings()))) {
            var report=r.value.refresh();assertTrue(report.success());assertEquals(Status.NOT_CONFIGURED,report.checks().getFirst().status());assertEquals("CACHED",r.value.cached().delivery_mode());assertEquals(1,calls.get());
        }
    }
    @Test void mandatoryUnconfiguredCannotBeReady() {
        HealthProbe p=new HealthProbe(){public String service(){return "required";}public String capability(){return "test";}public boolean requiredForReadiness(){return true;}public HealthCheckResult check(){return of("required","test",Status.NOT_CONFIGURED,SourceMode.NOT_CONFIGURED,"test","Missing","Configure","",true,Map.of());}};
        try(varRunner r=new varRunner(new HealthAggregator(List.of(p),settings()))){assertFalse(r.value.refresh().success());}
    }
    @Test void settingsRejectUnboundedConcurrency(){assertThrows(IllegalArgumentException.class,()->settings("parallelism","10000"));}
    @Test void concurrentRefreshesShareCompletedReportInsteadOfAnEmptyResult() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var calls=new AtomicInteger();
        var p=probe("shared",()->{calls.incrementAndGet();entered.countDown();release.await();return good("shared");});
        try(varRunner r=new varRunner(new HealthAggregator(List.of(p),settings()))) {
            var one=java.util.concurrent.CompletableFuture.supplyAsync(r.value::refresh);assertTrue(entered.await(1,java.util.concurrent.TimeUnit.SECONDS));
            var two=java.util.concurrent.CompletableFuture.supplyAsync(r.value::refresh);Thread.sleep(30);release.countDown();
            var first=one.get();var second=two.get();assertEquals(first.report_id(),second.report_id());assertEquals(1,calls.get());assertEquals(1,second.checks().size());assertTrue(second.success());
        }
    }
    private record varRunner(HealthAggregator value) implements AutoCloseable {public void close(){value.close();}}
}
