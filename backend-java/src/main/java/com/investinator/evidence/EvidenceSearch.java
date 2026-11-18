package com.investinator.evidence;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import com.investinator.api.ApiFailure;
import static com.investinator.domain.Json.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
@Service
@ConditionalOnProperty(name="investinator.evidence.enabled",havingValue="true")
public class EvidenceSearch {
    private final OpenSearchClient client;
    private final String index;
    public EvidenceSearch(OpenSearchClient client,@Value("${investinator.evidence.index:investinator-evidence}") String index) {
        this.client=client;this.index=index;
        if(!index.matches("[a-z][a-z0-9_-]{0,80}"))throw new IllegalArgumentException("A single configured evidence index is required");
    }
    public Map<String,Object> search(String tenant,String text,String service,String from,String to,int page,int size) {return query(tenant,null,text,service,from,to,page,size);}
    public Map<String,Object> searchForIncident(String tenant,String incident,String text){return query(tenant,incident,text,null,null,null,0,10);}
    private Map<String,Object> query(String tenant,String incident,String text,String service,String from,String to,int page,int size) {
        if(text==null||text.length()>200||text.contains("://")||text.strip().startsWith("{"))throw new ApiFailure(422,"Use bounded plain search text, not URLs or DSL");
        if(service!=null&&!service.matches("[A-Za-z0-9._-]{1,128}"))throw new ApiFailure(422,"Invalid service filter");
        if(size<1||size>50||page<0||(long)page*size>1000)throw new ApiFailure(422,"Pagination exceeds allowed bounds");
        Instant end=to==null?Instant.now():parse(to),start=from==null?end.minusSeconds(3600):parse(from);
        long seconds=Duration.between(start,end).getSeconds();
        if(seconds<0||seconds>14400||end.isAfter(Instant.now().plusSeconds(300)))throw new ApiFailure(422,"Time window must be at most four hours with bounded clock skew");
        var filters=new ArrayList<Query>();
        filters.add(Query.of(q->q.term(t->t.field("tenant_id").value(v->v.stringValue(tenant)))));
        filters.add(Query.of(q -> q.range(r -> r.field("@timestamp").gte(org.opensearch.client.json.JsonData.of(start.toString())).lte(org.opensearch.client.json.JsonData.of(end.toString())))));
        if(incident!=null)filters.add(Query.of(q->q.term(t->t.field("incident_id").value(v->v.stringValue(incident)))));
        if(service!=null)filters.add(Query.of(q->q.term(t->t.field("service.name").value(v->v.stringValue(service)))));
        Query query=Query.of(q->q.bool(b->{b.filter(filters);if(!text.isBlank())b.must(m->m.multiMatch(mm->mm.query(text).fields("event.original","service.name","error.signature")));return b;}));
        long before=System.nanoTime();
        try {
            var response=client.search(s->s.index(index).query(query).from(page*size).size(size).timeout("2s").terminateAfter(10000L)
                .sort(v->v.field(f->f.field("@timestamp").order(SortOrder.Desc))).sort(v->v.field(f->f.field("evidence_id").order(SortOrder.Asc)))
                .trackTotalHits(t->t.enabled(true)),NormalizedEvidence.class);
            var items=new ArrayList<NormalizedEvidence>();int invalid=0;
            for(var hit:response.hits().hits()) {
                var value=hit.source();if(value==null||!value.validFor(tenant,hit.id())){invalid++;continue;}items.add(value);
            }
            boolean partial=response.timedOut()||Boolean.TRUE.equals(response.terminatedEarly())||response.shards().failed()>0||invalid>0;
            return obj("status",partial?"DEGRADED":"HEALTHY","retrieval_mode","LIVE","checked_at",now(),"duration_ms",(System.nanoTime()-before)/1000000,"items",items,"page",page,"size",size,"total",response.hits().total()==null?null:response.hits().total().value(),"invalid_records",invalid,"partial",partial);
        }catch(OpenSearchException e){return unavailable(e.status()==429?"RATE_LIMITED":e.status()==401||e.status()==403?"UNAUTHORIZED":"UNAVAILABLE",before);}
        catch(Exception ignored){return unavailable("UNAVAILABLE",before);}
    }
    public Map<String,Object> health(String tenant) {
        long before=System.nanoTime();
        try {
            var cluster=client.cluster().health(h->h.timeout(t->t.time("2s")));
            var mapping=client.indices().getMapping(m->m.index(index));
            if(!mapping.result().containsKey(index)||!mapping.result().get(index).mappings().properties().containsKey("tenant_id"))return unavailable("SCHEMA_MISMATCH",before);
            var status=cluster.status().jsonValue();
            return obj("status",status.equals("red")?"UNHEALTHY":"HEALTHY","cluster_status",status,"retrieval_mode","LIVE","source_mode","NOT_TESTED","checked_at",now(),"duration_ms",(System.nanoTime()-before)/1000000,"note","Cluster/mapping observed; record provenance is separate");
        }catch(Exception ignored){return unavailable("UNAVAILABLE",before);}
    }
    private Map<String,Object> unavailable(String status,long before){return obj("status",status,"retrieval_mode","NOT_TESTED","checked_at",now(),"duration_ms",(System.nanoTime()-before)/1000000,"items",List.of(),"error_type",status,"next_safe_action","Check configured search credentials, service, mapping and collector diagnostics");}
    private Instant parse(String value){try{return OffsetDateTime.parse(value).toInstant();}catch(Exception e){throw new ApiFailure(422,"Use an ISO timestamp with timezone");}}
}
