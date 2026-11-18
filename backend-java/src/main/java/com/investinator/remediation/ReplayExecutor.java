package com.investinator.remediation;
import static com.investinator.domain.Json.*;
import com.investinator.persistence.Store;
import com.investinator.api.ApiFailure;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
/** Only isolated ReplayLab database state can change. No connector/shell/network executor exists. */
@Component @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class ReplayExecutor {
    private final Store db;
    public ReplayExecutor(Store db){this.db=db;}
    public void execute(String tenant,String planId,String lease,Map<String,Object> plan,String hash){db.write(tenant,()->{
        db.jdbc.execute("set local statement_timeout=5000");
        var fence=db.jdbc.queryForList("select id from remediation_plans where tenant_id=? and id=? and state='EXECUTING' and lease=? and lease_expires_at>now() and not revoked and approval_expires_at>now()",tenant,planId,lease);if(fence.isEmpty())throw new ApiFailure(409,"Execution lease/approval expired or revoked");
        String run=plan.get("run_id").toString();int expected=((Number)plan.get("expected_resource_version")).intValue();double value=((Number)plan.get("post_disk_percent")).doubleValue();
        if(db.jdbc.update("update replay_resources set version=version+1,disk_percent=?,observed_at=now() where tenant_id=? and run_id=? and version=? and source_mode='SIMULATED'",value,tenant,run,expected)!=1)throw new ApiFailure(409,"Replay resource fencing precondition changed");
        db.jdbc.update("insert into remediation_effects(tenant_id,plan_id,plan_hash,run_id,resource_version,disk_percent) values(?,?,?,?,?,?)",tenant,planId,hash,run,expected+1,value);return null;
    });}
    public Map<String,Object> sample(String tenant,String run){return db.write(tenant,()->{var rows=db.jdbc.queryForList("select version,disk_percent,source_mode from replay_resources where tenant_id=? and run_id=?",tenant,run);if(rows.isEmpty())throw new ApiFailure(404,"Owned ReplayLab state not found");var row=rows.getFirst();String id=id();db.jdbc.update("insert into remediation_samples(tenant_id,id,run_id,resource_version,disk_percent,source_mode) values(?,?,?,?,?,?)",tenant,id,run,row.get("version"),row.get("disk_percent"),row.get("source_mode"));return obj("sample_id",id,"source_mode","SIMULATED","resource_version",row.get("version"));});}
}
