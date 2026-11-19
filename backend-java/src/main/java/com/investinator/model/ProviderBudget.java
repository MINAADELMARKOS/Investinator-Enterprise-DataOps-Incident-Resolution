package com.investinator.model;

import java.math.*;
import java.util.*;
import java.util.function.Supplier;
import com.investinator.api.ApiFailure;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** USD-only durable ledger. Policies are deployment-owned; no API creates approvals. */
@Service @ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class ProviderBudget {
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final Environment env;
    public ProviderBudget(JdbcTemplate jdbc,Environment env){this.jdbc=jdbc;this.env=env;tx=new TransactionTemplate(new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource())));tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);tx.setTimeout(3);}
    private <T>T locked(Supplier<T> work){return tx.execute(s->{jdbc.execute("set local statement_timeout='2000ms'");jdbc.execute("select pg_advisory_xact_lock(hashtextextended('investinator:provider-budget',0))");return work.get();});}
    public boolean configured(){return env.getProperty("INVESTINATOR_OPENAI_PAID_OPT_IN",Boolean.class,false)&&env.getProperty("INVESTINATOR_OPENAI_COST_APPROVED",Boolean.class,false)&&!env.getProperty("INVESTINATOR_OPENAI_APPROVAL_REF","").isBlank();}
    public record Reservation(String id,long reserved_microusd) {}
    private static long cost(long input,long output,long inPrice,long outPrice){return BigDecimal.valueOf(input).multiply(BigDecimal.valueOf(inPrice)).add(BigDecimal.valueOf(output).multiply(BigDecimal.valueOf(outPrice))).divide(BigDecimal.valueOf(1000000),0,RoundingMode.CEILING).longValueExact();}
    public Reservation reserve(String tenant,String model,int maxInputTokens,int maxOutputTokens){
        if(!configured())throw new ApiFailure(403,"BUDGET_NOT_CONFIGURED");
        if(tenant==null||!tenant.matches("[A-Za-z0-9_.:-]{1,100}")||model==null||!model.matches("[A-Za-z0-9_.:-]{1,100}")||maxInputTokens<1||maxInputTokens>200000||maxOutputTokens<1||maxOutputTokens>2048)throw new ApiFailure(422,"BUDGET_BOUND_EXCEEDED");
        return locked(()->{
            String approval=env.getProperty("INVESTINATOR_OPENAI_APPROVAL_REF","");
            var prices=jdbc.queryForList("select input_microusd_per_million,output_microusd_per_million from provider_budget_prices where model=? and approval_ref=? and approved_until>now()",model,approval);
            if(prices.isEmpty())throw new ApiFailure(403,"PRICE_OR_APPROVAL_UNKNOWN");var price=prices.getFirst();long in=((Number)price.get("input_microusd_per_million")).longValue(),out=((Number)price.get("output_microusd_per_million")).longValue();long amount=cost(maxInputTokens,maxOutputTokens,in,out);
            var accounts=jdbc.queryForList("select tenant_id,model,limit_microusd,charged_microusd,reserved_microusd from provider_budget_accounts where ((tenant_id='*' and model='*') or (tenant_id=? and model=?)) and approval_ref=? and approved_until>now() for update",tenant,model,approval);
            if(accounts.size()!=2)throw new ApiFailure(403,"GLOBAL_OR_TENANT_BUDGET_UNKNOWN");
            for(var account:accounts){long available=Math.subtractExact(Math.subtractExact(((Number)account.get("limit_microusd")).longValue(),((Number)account.get("charged_microusd")).longValue()),((Number)account.get("reserved_microusd")).longValue());if(amount>available)throw new ApiFailure(429,"MONEY_BUDGET_EXCEEDED");}
            for(var account:accounts)jdbc.update("update provider_budget_accounts set reserved_microusd=reserved_microusd+? where tenant_id=? and model=?",amount,account.get("tenant_id"),account.get("model"));
            String id=UUID.randomUUID().toString();jdbc.update("insert into provider_budget_reservations(id,tenant_id,model,reserved_microusd,input_price,output_price,state,approval_ref) values(?,?,?,?,?,?,'RESERVED',?)",id,tenant,model,amount,in,out,approval);return new Reservation(id,amount);
        });
    }
    /** Missing usage/timeouts retain the full hold. UNKNOWN is not a zero-dollar completion. */
    public void reconcile(String tenant,String id,Long inputTokens,Long outputTokens){locked(()->{
        var rows=jdbc.queryForList("select * from provider_budget_reservations where id=? and tenant_id=? for update",id,tenant);if(rows.isEmpty())throw new ApiFailure(404,"Budget reservation not found");var row=rows.getFirst();
        boolean known=inputTokens!=null&&outputTokens!=null&&inputTokens>=0&&outputTokens>=0;
        if(Set.of("SETTLED","OVERDRAWN").contains(row.get("state"))){if(!known||!Objects.equals(row.get("input_tokens"),inputTokens)||!Objects.equals(row.get("output_tokens"),outputTokens))throw new ApiFailure(409,"Immutable provider receipt differs");return null;}
        if(!known){jdbc.update("update provider_budget_reservations set state='UNKNOWN' where id=?",id);return null;}
        long actual=cost(inputTokens,outputTokens,((Number)row.get("input_price")).longValue(),((Number)row.get("output_price")).longValue()),reserved=((Number)row.get("reserved_microusd")).longValue();
        jdbc.update("update provider_budget_accounts set reserved_microusd=reserved_microusd-?,charged_microusd=charged_microusd+? where (tenant_id='*' and model='*') or (tenant_id=? and model=?)",reserved,actual,tenant,row.get("model"));
        jdbc.update("update provider_budget_reservations set state=?,actual_microusd=?,input_tokens=?,output_tokens=?,reconciled_at=now() where id=?",actual>reserved?"OVERDRAWN":"SETTLED",actual,inputTokens,outputTokens,id);return null;
    });}
}
