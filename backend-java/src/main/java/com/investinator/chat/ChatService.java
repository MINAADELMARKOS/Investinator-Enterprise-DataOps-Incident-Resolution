package com.investinator.chat;
import static com.investinator.domain.Json.*;
import com.investinator.domain.Json;
import com.investinator.domain.Contracts.*;
import com.investinator.persistence.*;
import com.investinator.workflows.WorkflowService;
import com.investinator.api.ApiFailure;
import com.investinator.security.Redaction;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
@Service @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class ChatService {
    private final java.util.concurrent.Semaphore admitted=new java.util.concurrent.Semaphore(2);
    public Object admit(java.util.function.Supplier<Object> work){if(!admitted.tryAcquire())throw new ApiFailure(429,"Chat capacity reached; retry with the same idempotency key");try{return work.get();}finally{admitted.release();}}
    private final Store db;private final WorkflowService workflows;private final ChatGateway gateway;private final ReadTools tools;private final StateCipher cipher;
    public ChatService(Store db,WorkflowService workflows,ChatGateway gateway,ReadTools tools,StateCipher cipher){this.db=db;this.workflows=workflows;this.gateway=gateway;this.tools=tools;this.cipher=cipher;}
    public String configurationStatus(){return gateway.configurationStatus();}
    public Map<String,Object> handle(String tenant,ChatMessage input) {
        String conversation=input.conversation_id(),incidentId=input.incident_id();boolean scopeChanged=false;
        if(conversation!=null){var existing=db.select(Table.CONVERSATIONS,tenant,"id=?",conversation);if(existing.isEmpty())throw new ApiFailure(404,"Conversation not found");String bound=(String)existing.getFirst().get("incident_id");if(incidentId==null)incidentId=bound;else if(bound!=null&&!bound.equals(incidentId))throw new ApiFailure(409,"Start a new conversation when changing incident scope");else if(bound==null){db.update(Table.CONVERSATIONS,tenant,conversation,obj("incident_id",incidentId));scopeChanged=true;}}
        else {conversation=id();db.insert(Table.CONVERSATIONS,tenant,obj("id",conversation,"incident_id",incidentId));}
        var incident=incidentId==null?null:workflows.incident(tenant,incidentId);
        var history=new ArrayList<>(db.select(Table.MESSAGES,tenant,"conversation_id=?",conversation));Collections.reverse(history);
        String fallback=incident==null?"Select an incident to investigate evidence, impact, and safe next steps.":input.mode().equals("fix")?"Proposed actions are shown in the Incident Room. Human approval is required for risky steps.":incident.executive_summary;
        var context=new ReadTools.Context(tenant,incidentId,incidentId==null?"NOT_TESTED":"SIMULATED");tools.initialize(context);
        List<com.openai.models.responses.ResponseInputItem> previous=List.of();String epoch=tools.epoch(tenant);
        var state=db.jdbc.queryForList("select ciphertext from provider_conversation_state where tenant_id=? and conversation_id=?",String.class,tenant,conversation);
        if(cipher.configured()&&!scopeChanged&&!state.isEmpty())try{var envelope=map(Json.decode(cipher.decrypt(tenant,conversation,state.getFirst())));if(epoch.equals(envelope.get("policy_epoch")))previous=ChatGateway.deserialize(envelope.get("items").toString());else history.removeIf(message->!"user".equals(message.get("role")));}catch(Exception ignored){previous=List.of();}
        db.insert(Table.MESSAGES,tenant,obj("conversation_id",conversation,"role","user","content",Redaction.redact(input.message()),"payload",obj("mode",input.mode())));
        long before=System.nanoTime();var result=gateway.turn(context,input.message(),input.mode(),incident==null?"P4":incident.severity.name(),history,previous,fallback);
        var ids=result.analysis()==null?(incident==null?List.of():incident.evidence.stream().limit(5).map(Evidence::id).toList()):result.analysis().observed_evidence().stream().map(InvestigationAnswer.Citation::evidence_id).distinct().toList();
        long elapsed=(System.nanoTime()-before)/1000000;
        Object usage=result.input_tokens()+result.output_tokens()>0?obj("input_tokens",result.input_tokens(),"output_tokens",result.output_tokens(),"requests",result.requests(),"latency_ms",elapsed,"cost",null):null;
        var metadata=obj("response_mode",result.response_mode(),"provider_status",result.provider_status(),"source_mode",context.sourceMode,"analysis",result.analysis(),"tool_trace",result.tool_trace(),"model",result.requests()>0?result.model():null,"usage",usage);
        db.insert(Table.MESSAGES,tenant,obj("conversation_id",conversation,"role","assistant","content",result.answer(),"payload",obj("evidence_ids",ids,"mode",result.response_mode(),"turn_metadata",metadata)));
        if(result.response_mode().equals("gpt")){String privateText=encode(obj("policy_epoch",epoch,"items",ChatGateway.serialize(result.continuation())));db.jdbc.update("insert into provider_conversation_state(tenant_id,conversation_id,ciphertext) values(?,?,?) on conflict(tenant_id,conversation_id) do update set ciphertext=excluded.ciphertext,updated_at=now()",tenant,conversation,cipher.encrypt(tenant,conversation,privateText));}
        else db.jdbc.update("delete from provider_conversation_state where tenant_id=? and conversation_id=?",tenant,conversation);
        if(result.requests()>0)db.insert(Table.AI_USAGE,tenant,obj("incident_id",incidentId,"task","investigator:"+input.mode(),"model",result.model(),"latency_ms",(int)((System.nanoTime()-before)/1000000),"input_tokens",result.input_tokens(),"output_tokens",result.output_tokens(),"status",result.provider_status().equals("OK")?"ok":result.provider_status(),"request_count",result.requests()));
        workflows.audit(tenant,"chat_message",incidentId,"demo.senior_sre",obj("conversation_id",conversation,"mode",input.mode()));
        if(result.requests()>0)workflows.audit(tenant,"ai_provider_turn",incidentId,"demo.senior_sre",obj("conversation_id",conversation,"response_mode",result.response_mode(),"provider_status",result.provider_status(),"requests",result.requests(),"tools",result.tool_trace()));
        return obj("conversation_id",conversation,"incident_id",incidentId,"answer",result.answer(),"evidence_ids",ids,"response_mode",result.response_mode(),"provider_status",result.provider_status(),"source_mode",context.sourceMode,"analysis",result.analysis(),"tool_trace",result.tool_trace(),"model",result.requests()>0?result.model():null,"usage",usage);
    }
}
