package com.investinator.model;
import static com.investinator.domain.Json.*;
import org.springframework.core.env.StandardEnvironment;
import com.openai.models.responses.*;
import com.investinator.security.Redaction;
/** Invoked only by explicit CLI --live. No incident/log data; one small Responses SDK request. */
public final class OpenAiLiveProbe {
    public static void main(String[] args) {
        if(args.length!=1||!args[0].equals("--live"))throw new IllegalArgumentException("Explicit --live is required");
        var env=new StandardEnvironment();String model=env.getProperty("INVESTINATOR_OPENAI_MODEL","");
        if(model.isBlank()||env.getProperty("OPENAI_API_KEY","").isBlank()){System.out.println(encode(obj("status","NOT_CONFIGURED","paid_call_made",false)));return;}
        var client=ProviderClients.create(env);
        try {
            var response=client.responses().create(ResponseCreateParams.builder().model(model).input("Reply with the single word OK.").store(false).maxOutputTokens(64).build());
            String text=response.output().stream().filter(ResponseOutputItem::isMessage).flatMap(i->i.asMessage().content().stream()).filter(c->c.isOutputText()).map(c->c.asOutputText().text()).reduce("",String::concat);
            boolean ok=response.status().map(s->s.toString().equals("completed")).orElse(false)&&!text.isBlank();
            System.out.println(encode(obj("status",ok?"HEALTHY":"UNHEALTHY","source_mode","LIVE","paid_call_made",true,"text",Redaction.redact(text),"error_type",ok?"":"NO_COMPLETED_TEXT")));
        }catch(Exception error){System.out.println(encode(obj("status","UNHEALTHY","paid_call_made",true,"error_type",error.getClass().getSimpleName())));}finally{client.close();}
    }
}
