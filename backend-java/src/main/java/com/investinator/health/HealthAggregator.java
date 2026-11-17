package com.investinator.health;
import static com.investinator.health.HealthCheckResult.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.time.Instant;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
@Service
public class HealthAggregator {
    public record Report(String report_id,String checked_at,String delivery_mode,boolean success,boolean partial,List<HealthCheckResult> checks) {}
    private static class Circuit {int failures;long openedUntil;}
    private final List<HealthProbe> probes;
    private final HealthSettings settings;
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor timers;
    private final Map<String,Circuit> circuits=new ConcurrentHashMap<>();
    private volatile Report latest;
    private final AtomicReference<CompletableFuture<Report>> inFlight=new AtomicReference<>();
    @org.springframework.beans.factory.annotation.Autowired
    public HealthAggregator(ProbeCatalog catalog,HealthSettings settings){this(catalog.probes(),settings);}
    public HealthAggregator(List<HealthProbe> probes,HealthSettings settings) {
        this.probes=List.copyOf(probes);this.settings=settings;
        workers=new ThreadPoolExecutor(settings.parallelism,settings.parallelism,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(settings.queueSize),Thread.ofPlatform().daemon(true).name("health-worker-",0).factory());
        timers=new ScheduledThreadPoolExecutor(2,Thread.ofPlatform().daemon(true).name("health-timer-",0).factory());timers.setRemoveOnCancelPolicy(true);
        if(settings.scheduled)timers.scheduleWithFixedDelay(()->{try{refresh();}catch(Exception ignored){}},settings.intervalMs,settings.intervalMs,TimeUnit.MILLISECONDS);
    }
    public Report cached(){
        var value=latest;if(value==null)return refresh();
        boolean stale=java.time.Duration.between(Instant.parse(value.checked_at),Instant.now()).toMillis()>settings.intervalMs*2L;
        var checks=stale?value.checks.stream().map(r->HealthCheckResult.of(r.service(),r.capability(),Status.UNKNOWN,SourceMode.CACHED,r.evidence_source(),"Cached diagnostic expired","Request a fresh diagnostic","STALE_CACHE",r.required_for_readiness(),Map.of())).toList():value.checks;
        return new Report(value.report_id,value.checked_at,"CACHED",stale?false:value.success,stale||value.partial,checks);
    }
    public Report refresh() {
        var running=new CompletableFuture<Report>();
        var active=inFlight.compareAndExchange(null,running);
        if(active!=null) {
            if(active!=null)try{return active.get(settings.deadlineMs,TimeUnit.MILLISECONDS);}catch(Exception ignored){}
            return new Report(UUID.randomUUID().toString(),Instant.now().toString(),"LIVE",false,true,probes.stream().map(p->failure(p,Status.UNKNOWN,"REFRESH_IN_PROGRESS",0)).toList());
        }
        try {
            var pending=new LinkedHashMap<HealthProbe,CompletableFuture<HealthCheckResult>>();
            for(var probe:probes)pending.put(probe,submit(probe));
            long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(settings.deadlineMs);
            var checks=new ArrayList<HealthCheckResult>();boolean partial=false;
            for(var item:pending.entrySet()) {
                try{checks.add(item.getValue().get(Math.max(1,end-System.nanoTime()),TimeUnit.NANOSECONDS));}
                catch(Exception error){partial=true;checks.add(failure(item.getKey(),Status.UNKNOWN,"DEADLINE_EXCEEDED",settings.deadlineMs));}
            }
            partial|=checks.stream().anyMatch(r->Set.of("TIMEOUT","DEADLINE_EXCEEDED","SATURATED","CIRCUIT_OPEN").contains(r.error_type()));
            boolean success=checks.stream().noneMatch(HealthCheckResult::failed)&&checks.stream().noneMatch(r->r.required_for_readiness()&&r.status()!=Status.HEALTHY);
            latest=new Report(UUID.randomUUID().toString(),Instant.now().toString(),"LIVE",success,partial,List.copyOf(checks));workers.purge();running.complete(latest);return latest;
        }finally{if(!running.isDone())running.completeExceptionally(new IllegalStateException("Diagnostic refresh did not finish"));inFlight.compareAndSet(running,null);}
    }
    public HealthCheckResult diagnose(HealthProbe probe){return diagnose(probe,settings.timeoutMs+200L);}
    public HealthCheckResult diagnose(HealthProbe probe,long waitMs){var pending=submit(probe);try{return pending.get(Math.min(waitMs,settings.timeoutMs+200L),TimeUnit.MILLISECONDS);}catch(Exception e){pending.cancel(true);return failure(probe,Status.UNKNOWN,"DEADLINE_EXCEEDED",Math.min(waitMs,settings.timeoutMs));}}
    private CompletableFuture<HealthCheckResult> submit(HealthProbe probe) {
        var result=new CompletableFuture<HealthCheckResult>();var circuit=circuits.computeIfAbsent(probe.service(),key->new Circuit());
        synchronized(circuit){if(System.nanoTime()<circuit.openedUntil){result.complete(failure(probe,Status.UNKNOWN,"CIRCUIT_OPEN",0));return result;}}
        long before=System.nanoTime();
        AtomicReference<Future<?>> handle=new AtomicReference<>();
        var timer=timers.schedule(()->{synchronized(result){if(!result.isDone()){record(circuit,false);result.complete(failure(probe,Status.UNHEALTHY,"TIMEOUT",settings.timeoutMs));var task=handle.get();if(task!=null)task.cancel(true);}}},settings.timeoutMs,TimeUnit.MILLISECONDS);
        try {
            handle.set(workers.submit(()->{
                HealthCheckResult value=null;int attempts=0;
                do {
                    attempts++;
                    try{value=probe.check();}
                    catch(InterruptedException e){Thread.currentThread().interrupt();return;}
                    catch(Exception error){value=failure(probe,Status.UNHEALTHY,error.getClass().getSimpleName(),0);}
                    if(!value.failed()||!Set.of("IOException","ConnectException","HttpTimeoutException","HTTP_5XX").contains(value.error_type())||attempts>settings.retries)break;
                    try{Thread.sleep(ThreadLocalRandom.current().nextInt(10,80));}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
                }while(!result.isDone());
                synchronized(result){if(value!=null&&!result.isDone()){record(circuit,!value.failed());result.complete(value.timed(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-before),attempts));}}
            }));
            if(result.isDone())handle.get().cancel(true);
        }catch(RejectedExecutionException ignored){result.complete(failure(probe,Status.UNKNOWN,"SATURATED",0));}
        result.whenComplete((value,error)->{timer.cancel(false);if(result.isCancelled()){var task=handle.get();if(task!=null)task.cancel(true);}});return result;
    }
    private void record(Circuit circuit,boolean success){synchronized(circuit){if(success){circuit.failures=0;circuit.openedUntil=0;}else if(++circuit.failures>=settings.failureThreshold){circuit.openedUntil=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(settings.cooldownMs);circuit.failures=0;}}}
    private HealthCheckResult failure(HealthProbe probe,Status status,String error,long duration) {
        return HealthCheckResult.of(probe.service(),probe.capability(),status,SourceMode.LIVE,"bounded diagnostic runner","Diagnostic did not complete successfully","Check the configured service and credentials; retry after cooldown",error,probe.requiredForReadiness(),Map.of()).timed(duration,0);
    }
    @PreDestroy public void close(){workers.shutdownNow();timers.shutdownNow();}
}
