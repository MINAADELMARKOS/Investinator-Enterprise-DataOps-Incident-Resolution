package com.investinator.remediation;
import java.util.*;
import com.investinator.api.ApiFailure;
public final class ActionRegistry {
    private ActionRegistry(){}
    public enum State {PROPOSED,AUTHORIZED,EXECUTING,EXECUTED,VERIFIED,FAILED,REQUIRES_MANUAL_INTERVENTION,ROLLED_BACK}
    public record Definition(String code,String environment,String platform,String version,String risk,String owner,String required_role,String rollback,int timeout_seconds,int approval_seconds,boolean two_person){}
    public record PolicyDecision(boolean proposal_eligible,boolean execution_authorized,String reason,String required_role,String source_mode){}
    public record Verification(String plan_id,boolean passed,String sampled_at,int resource_version,String source_mode,String verifier){}
    public record Postmortem(String plan_id,String incident_id,boolean rollback,Verification verification,String review_state){}
    public static final Definition RECOVER=new Definition("REPLAY_DISK_RECOVER_V1","ReplayLab","Kafka","4.3","LOW_SIMULATION_ONLY","ReplayLab","SIMULATION_APPROVER","REPLAY_DISK_RESTORE_V1",10,120,false);
    public static final Definition RESTORE=new Definition("REPLAY_DISK_RESTORE_V1","ReplayLab","Kafka","4.3","LOW_SIMULATION_ONLY","ReplayLab","SIMULATION_APPROVER","NONE",10,120,false);
    public static Definition definition(String code){return switch(code){case "REPLAY_DISK_RECOVER_V1"->RECOVER;case "REPLAY_DISK_RESTORE_V1"->RESTORE;default->throw new ApiFailure(403,"Unknown action; real write executors are disabled");};}
    public static void arguments(String code,Map<String,Object> values){if(code.equals(RECOVER.code())){if(!values.keySet().equals(Set.of("disk_percent"))||!(values.get("disk_percent") instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()<10||n.doubleValue()>=80)throw new ApiFailure(422,"Recovery accepts only bounded simulated disk_percent [10,80)");}else if(!values.isEmpty())throw new ApiFailure(422,"Named rollback takes no inferred parameters");}
}
