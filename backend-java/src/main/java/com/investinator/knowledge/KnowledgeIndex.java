package com.investinator.knowledge;
import com.investinator.config.EvidenceConfiguration;
import com.investinator.api.ApiFailure;
import org.opensearch.client.opensearch.*;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import jakarta.annotation.PreDestroy;
import java.util.*;
/** BM25 only. Operator bootstrap owns mappings/identities; application writer only creates approved chunks. */
@Component
public class KnowledgeIndex {
    public static final String INDEX="investinator-knowledge";
    private final ObjectProvider<OpenSearchClient> readers;private final Environment env;private OpenSearchTransport transport;private OpenSearchClient writer;
    public KnowledgeIndex(ObjectProvider<OpenSearchClient> readers,Environment env){this.readers=readers;this.env=env;}
    public boolean configured(){return readers.getIfAvailable()!=null;}
    private synchronized OpenSearchClient writer() {
        if(writer!=null)return writer;
        String user=env.getProperty("KNOWLEDGE_WRITER_USER",""),password=env.getProperty("KNOWLEDGE_WRITER_PASSWORD","");if(user.isBlank()||password.isBlank())throw new ApiFailure(503,"Knowledge index writer is not configured");
        transport=new EvidenceConfiguration().evidenceTransport(env.getRequiredProperty("investinator.evidence.url"),user,password,env.getProperty("investinator.demo-mode",Boolean.class,true));writer=new OpenSearchClient(transport);return writer;
    }
    public void publish(List<Map<String,Object>> chunks) {
        if(chunks.size()>32)throw new ApiFailure(422,"Knowledge chunk limit");
        try{for(var chunk:chunks)try{writer().index(i->i.index(INDEX).id(chunk.get("chunk_id").toString()).opType(OpType.Create).refresh(Refresh.WaitFor).document(chunk));}catch(Exception e){if(status(e)!=409)throw e;var reader=readers.getIfAvailable();if(reader==null)throw new ApiFailure(503,"Cannot verify an existing chunk without the scoped reader");var existing=reader.get(g->g.index(INDEX).id(chunk.get("chunk_id").toString()),Map.class).source();if(existing==null||!Objects.equals(existing.get("content_hash"),chunk.get("content_hash"))||!Objects.equals(existing.get("text"),chunk.get("text"))||!Objects.equals(existing.get("tenant_id"),chunk.get("tenant_id")))throw new ApiFailure(409,"Existing index chunk conflicts with approved content; reviewed repair required");}}
        catch(ApiFailure e){throw e;}catch(org.opensearch.client.opensearch._types.OpenSearchException e){throw new ApiFailure(503,"Knowledge indexing unavailable: "+e.status()+"/"+e.error().type()+"; approved revision is pending publication");}catch(Exception e){var failure=new ApiFailure(503,"Knowledge indexing unavailable: "+e.getClass().getSimpleName()+"; approved revision is pending publication");failure.initCause(e);throw failure;}
    }
    private static int status(Throwable error){for(Throwable cause=error;cause!=null;cause=cause.getCause()){if(cause instanceof org.opensearch.client.transport.httpclient5.ResponseException response)return response.status();if(cause instanceof org.opensearch.client.opensearch._types.OpenSearchException response)return response.status();}return -1;}
    public record Hit(Map<String,Object> document,double score) {}
    public record Retrieval(String status,List<Hit> hits,boolean partial) {}
    public Retrieval search(String tenant,String principal,String text,String technology,String version,String mode,List<String> currentChunkIds) {
        var client=readers.getIfAvailable();if(client==null)return new Retrieval("NOT_CONFIGURED",List.of(),true);
        if(text==null||text.isBlank()||text.length()>200||text.contains("://")||text.strip().startsWith("{"))throw new ApiFailure(422,"Use bounded plain knowledge search text");
        if(currentChunkIds.size()>4096)throw new ApiFailure(422,"Knowledge retrieval candidate budget");
        var filters=new ArrayList<Query>();filters.add(currentChunkIds.isEmpty()?Query.of(q->q.matchNone(m->m)):Query.of(q->q.terms(t->t.field("chunk_id").terms(v->v.value(currentChunkIds.stream().map(org.opensearch.client.opensearch._types.FieldValue::of).toList())))));filters.add(term("tenant_id",tenant));filters.add(term("review_state","APPROVED"));filters.add(term("acl_principals",principal));filters.add(term("technology",technology));
        var versions=new ArrayList<String>();versions.add("ANY");if(!version.equals("UNKNOWN")){String accumulated="";for(String part:version.split("\\.")){accumulated=accumulated.isEmpty()?part:accumulated+"."+part;versions.add(accumulated);}}filters.add(Query.of(q->q.bool(b->{versions.forEach(v->b.should(term("platform_version",v)));return b.minimumShouldMatch("1");})));filters.add(Query.of(q->q.bool(b->b.should(term("metadata_mode","ANY")).should(term("metadata_mode",mode)).minimumShouldMatch("1"))));
        var query=Query.of(q->q.bool(b->b.filter(filters).must(m->m.multiMatch(mm->mm.query(text).fields("title^3","text"))).should(term("platform_version",version)).should(term("metadata_mode",mode))));
        try{var result=client.search(s->s.index(INDEX).query(query).size(30).sort(v->v.score(sc->sc.order(org.opensearch.client.opensearch._types.SortOrder.Desc))).sort(v->v.field(f->f.field("chunk_id").order(org.opensearch.client.opensearch._types.SortOrder.Asc))).timeout("2s").terminateAfter(10000L),Map.class);var hits=new ArrayList<Hit>();for(var hit:result.hits().hits())if(hit.source()!=null)hits.add(new Hit(hit.source(),hit.score()==null?0:hit.score()));boolean partial=(result.hits().total()!=null&&result.hits().total().value()>30)||result.timedOut()||Boolean.TRUE.equals(result.terminatedEarly())||result.shards().failed()>0;return new Retrieval(partial?"DEGRADED":"HEALTHY",hits,partial);}catch(Exception error){return new Retrieval("UNAVAILABLE",List.of(),true);}
    }
    private Query term(String field,String value){return Query.of(q->q.term(t->t.field(field).value(v->v.stringValue(value))));}
    @PreDestroy public synchronized void close()throws Exception{if(transport!=null)transport.close();}
}
