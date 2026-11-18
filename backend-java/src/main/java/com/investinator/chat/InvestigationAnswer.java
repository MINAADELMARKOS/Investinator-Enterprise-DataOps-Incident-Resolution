package com.investinator.chat;
import java.util.*;
/** Public structured output; reasoning/approval records are intentionally absent. */
public record InvestigationAnswer(String summary,List<Citation> observed_evidence,List<Cause> probable_originating_causes,
    List<String> alternatives,List<String> missing_checks,String business_impact,List<String> recommended_read_only_checks,
    List<String> preventive_recommendations,String safe_remediation_proposal,String source_mode) {
    public record Citation(String evidence_id,String source,String observed_at,String kind) {}
    public record Cause(String statement,List<String> evidence_ids) {}
    public InvestigationAnswer {if(summary==null||summary.isBlank()||summary.length()>6000||observed_evidence==null||probable_originating_causes==null||alternatives==null||missing_checks==null||business_impact==null||recommended_read_only_checks==null||preventive_recommendations==null||safe_remediation_proposal==null||source_mode==null)throw new IllegalArgumentException("Incomplete typed investigation");}
}
