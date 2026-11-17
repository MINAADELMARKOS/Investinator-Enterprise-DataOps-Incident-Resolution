package com.investinator.connectors;
import com.investinator.health.ProbeHttp;
import java.util.*;
import org.ietf.jgss.*;
/** GET-only transport. Redirects, unverifiable TLS and unbounded bodies are refused. */
public class ConnectorHttp {
    private final ProbeHttp http=new ProbeHttp();
    public ProbeHttp.Reply get(ConnectorConfig config,String path)throws Exception {
        config.endpoint();
        if(!config.auth().equals("SPNEGO"))return http.get(config.value("URL"),path,config.headers());
        var manager=GSSManager.getInstance();var principal=manager.createName("HTTP@"+config.endpoint().getHost(),GSSName.NT_HOSTBASED_SERVICE);
        var context=manager.createContext(principal,new Oid("1.3.6.1.5.5.2"),null,GSSContext.DEFAULT_LIFETIME);
        try {
            context.requestMutualAuth(true);context.requestCredDeleg(false);byte[] challenge=new byte[0];
            for(int round=0;round<3;round++) {
                byte[] token=context.initSecContext(challenge,0,challenge.length);
                if(token==null)throw new SecurityException("SPNEGO token unavailable");
                var reply=http.get(config.value("URL"),path,Map.of("Authorization","Negotiate "+Base64.getEncoder().encodeToString(token)));
                var negotiate=reply.headers().getOrDefault("www-authenticate",List.of()).stream().filter(h->h.startsWith("Negotiate ")).findFirst();
                if(negotiate.isPresent()) {
                    String value=negotiate.get().substring(10);if(value.length()>16384)throw new SecurityException("SPNEGO response limit");challenge=Base64.getDecoder().decode(value);
                    if(reply.status()!=401){context.initSecContext(challenge,0,challenge.length);if(!context.isEstablished())throw new SecurityException("SPNEGO mutual authentication incomplete");return reply;}
                } else {
                    if(reply.status()==200&&!context.isEstablished())throw new SecurityException("SPNEGO mutual authentication incomplete");return reply;
                }
            }
            throw new SecurityException("SPNEGO negotiation limit");
        }finally{context.dispose();}
    }
}
