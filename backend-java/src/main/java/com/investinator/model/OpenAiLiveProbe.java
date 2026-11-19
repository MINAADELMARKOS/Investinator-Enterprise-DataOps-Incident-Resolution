package com.investinator.model;
import static com.investinator.domain.Json.*;
import org.springframework.core.env.StandardEnvironment;
/** Budget-only preflight. Standalone billing is denied: requests must use the durable backend ledger. */
public final class OpenAiLiveProbe {
    public static void main(String[] args) {
        if(args.length!=1||!java.util.Set.of("--live","--preflight").contains(args[0]))throw new IllegalArgumentException("Explicit preflight or live intent required");
        var env=new StandardEnvironment();
        boolean key=!env.getProperty("OPENAI_API_KEY","").isBlank(),model=!env.getProperty("INVESTINATOR_OPENAI_MODEL","").isBlank(),enabled=env.getProperty("INVESTINATOR_OPENAI_ENABLED",Boolean.class,false),approved=env.getProperty("INVESTINATOR_OPENAI_COST_APPROVED",Boolean.class,false)&&env.getProperty("INVESTINATOR_OPENAI_PAID_OPT_IN",Boolean.class,false)&&!env.getProperty("INVESTINATOR_OPENAI_APPROVAL_REF","").isBlank();
        System.out.println(encode(obj("status",key&&model&&enabled&&approved?"BLOCKED_DURABLE_LEDGER_REQUIRED":"NOT_CONFIGURED","key_present",key,"model_present",model,"enabled",enabled,"budget_approval_config_present",approved,"credential_validity","NOT_TESTED","paid_call_made",false,"reason","Standalone probe has no durable reservation; use separately approved ledger-backed backend only")));
    }
}
