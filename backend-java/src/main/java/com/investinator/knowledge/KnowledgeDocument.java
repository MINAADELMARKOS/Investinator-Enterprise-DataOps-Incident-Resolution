package com.investinator.knowledge;
import com.investinator.security.Redaction;
import com.investinator.agent.DeterministicInvestigator;
import static com.investinator.domain.Json.*;
import java.time.*;
import java.net.URI;
import java.util.*;
public record KnowledgeDocument(String document_id,int revision,Kind source_type,String title,String technology,String platform_version,
    String metadata_mode,String source,String owner,String source_time,List<String> acl_principals,String content,
    String incident_id,String graph_id,Integer graph_revision,String feedback_id,String source_mode) {
    public enum Kind { RUNBOOK,RESOLVED_POSTMORTEM,KNOWN_ERROR,TOPOLOGY_SNAPSHOT,CONFIG_CHANGE,VALIDATED_CORRECTIVE_ACTION }
    public KnowledgeDocument {
        platform_version=platform_version==null?"ANY":platform_version;metadata_mode=metadata_mode==null?"ANY":metadata_mode;source_mode=source_mode==null?"HISTORICAL_ADVICE":source_mode;
        if(document_id==null||!document_id.matches("[A-Za-z0-9_.-]{1,100}")||revision<1||source_type==null||title==null||title.isBlank()||title.length()>200||technology==null||!technology.matches("[A-Za-z0-9_-]{1,50}")||!platform_version.matches("ANY|[0-9]{1,3}(\\.[0-9]{1,3}){0,2}")||!Set.of("ANY","KRAFT","LEGACY_ZOOKEEPER","UNKNOWN").contains(metadata_mode)||source==null||source.length()>500||owner==null||owner.isBlank()||owner.length()>150||content==null||content.isBlank()||content.length()>48000||acl_principals==null||acl_principals.isEmpty()||acl_principals.size()>32||!Set.of("SIMULATED","HISTORICAL_ADVICE").contains(source_mode))throw new IllegalArgumentException("Invalid versioned knowledge document");
        Instant time=Instant.parse(source_time);source_time=time.toString();if(time.isAfter(Instant.now().plusSeconds(300)))throw new IllegalArgumentException("Knowledge source time is in the future");
        if(source.startsWith("https://")){var uri=URI.create(source);if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null)throw new IllegalArgumentException("Unsafe source reference");}else if(!source.matches("(?:repo|incident|graph|change):[A-Za-z0-9_./:@#-]+"))throw new IllegalArgumentException("Source is a reference, never an arbitrary fetch URL");
        content=Redaction.redact(content.replace("\r\n","\n").replace("\r","\n").strip());title=Redaction.redact(title.strip());owner=Redaction.redact(owner.strip());
        if(acl_principals.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Invalid knowledge ACL");
        acl_principals=acl_principals.stream().distinct().sorted().toList();
    }
    public String hash(){return DeterministicInvestigator.digest(encode(this));}
    public boolean quarantined(){return java.util.regex.Pattern.compile("(?i)ignore (?:all |previous |system )*(?:instructions|policy)|reveal (?:the )?(?:api|secret)|bypass (?:approval|policy)|you are (?:the )?system|approval (?:is )?granted").matcher(content).find();}
    public List<Map<String,Object>> chunks(String tenant) {
        var chunks=new ArrayList<Map<String,Object>>();int position=0;for(int start=0;start<content.length();start+=1800) {
            int end=Math.min(content.length(),start+2000);String id="KB-"+DeterministicInvestigator.digest(tenant+"|"+document_id+"|"+revision+"|"+hash()+"|"+position);
            chunks.add(obj("chunk_id",id,"tenant_id",tenant,"document_id",document_id,"revision",revision,"content_hash",hash(),"chunk_index",position++,"text",content.substring(start,end),"title",title,"technology",technology,"platform_version",platform_version,"metadata_mode",metadata_mode,"source",source,"owner",owner,"source_time",source_time,"acl_principals",acl_principals,"review_state","APPROVED","source_type",source_type,"source_mode",source_mode));if(end==content.length())break;
        }return List.copyOf(chunks);
    }
}
