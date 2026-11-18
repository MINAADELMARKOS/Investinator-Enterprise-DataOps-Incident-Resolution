package com.investinator.chat;
import static com.investinator.domain.Json.*;
import com.investinator.domain.Json;
import com.investinator.persistence.*;
import com.investinator.workflows.WorkflowService;
import com.investinator.knowledge.*;
import com.investinator.correlation.CorrelationService;
import com.investinator.security.Redaction;
import com.investinator.api.ApiFailure;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
@Service @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class ConversationReadService {
    private final Store db;private final ReadTools tools;private final WorkflowService workflows;private final CorrelationService correlations;private final KnowledgeService knowledge;
    public ConversationReadService(Store db,ReadTools tools,WorkflowService workflows,CorrelationService correlations,KnowledgeService knowledge){this.db=db;this.tools=tools;this.workflows=workflows;this.correlations=correlations;this.knowledge=knowledge;}
    public Object list(String tenant){return obj("items",db.all(Table.CONVERSATIONS,tenant).stream().limit(50).map(r->obj("id",r.get("id"),"incident_id",r.get("incident_id"),"created_at",r.get("created_at"))).toList());}
    public Object inspect(String tenant,String conversation,String id){var rows=db.select(Table.CONVERSATIONS,tenant,"id=?",conversation);if(rows.isEmpty()||rows.getFirst().get("incident_id")==null)throw new ApiFailure(404,"Conversation evidence unavailable");String incident=rows.getFirst().get("incident_id").toString();var context=new ReadTools.Context(tenant,incident,"SIMULATED");tools.initialize(context);var citation=context.citations.get(id);if(citation==null){var stored=db.jdbc.queryForList("select payload::text from citation_evidence where tenant_id=? and incident_id=? and evidence_id=?",String.class,tenant,incident,id);if(!stored.isEmpty())citation=Json.decode(encode(map(Json.decode(stored.getFirst())).get("citation")),InvestigationAnswer.Citation.class);}if(citation==null||!tools.citationAllowed(context,citation))throw new ApiFailure(404,"Evidence is unavailable or no longer authorized");
        var base=workflows.incident(tenant,incident).evidence.stream().filter(e->e.id().equals(id)).findFirst();if(base.isPresent())return Redaction.object(obj("citation",citation,"snippet",base.get().snippet(),"source_mode","SIMULATED","historical_advice",citation.kind().equals("HISTORICAL_ADVICE")));
        var observation=correlations.observations(tenant,incident).stream().filter(e->e.id().equals(id)).findFirst();if(observation.isPresent())return obj("citation",citation,"observation",observation.get(),"source_mode",observation.get().source_mode(),"historical_advice",false);
        var values=db.jdbc.queryForList("select payload::text from citation_evidence where tenant_id=? and incident_id=? and evidence_id=?",String.class,tenant,incident,id);if(values.isEmpty())throw new ApiFailure(404,"Evidence unavailable");var facts=map(map(Json.decode(values.getFirst())).get("facts"));if("knowledge".equals(facts.get("kind"))){var readable=knowledge.readableDocument(tenant,context.principal,facts.get("document_id").toString(),((Number)facts.get("revision")).intValue());var document=(KnowledgeDocument)readable.get("document");var chunk=document.chunks(tenant).stream().filter(c->c.get("chunk_id").equals(id)).findFirst().orElseThrow(()->new ApiFailure(404,"Knowledge chunk unavailable"));return obj("citation",citation,"snippet",chunk.get("text"),"source",chunk.get("source"),"owner",chunk.get("owner"),"source_mode","HISTORICAL_ADVICE","historical_advice",true);}
        return Redaction.object(obj("citation",citation,"facts",facts,"source_mode",facts.getOrDefault("source_mode","NOT_TESTED"),"historical_advice",false));
    }
}
