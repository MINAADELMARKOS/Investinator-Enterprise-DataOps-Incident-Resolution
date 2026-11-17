package com.investinator.config;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
@Configuration
@ConditionalOnProperty(name="investinator.evidence.enabled",havingValue="true")
public class EvidenceConfiguration {
    @Bean(destroyMethod="close") OpenSearchTransport evidenceTransport(
        @Value("${investinator.evidence.url}") String url,@Value("${investinator.evidence.username}") String username,
        @Value("${investinator.evidence.password}") String password,@Value("${investinator.demo-mode:true}") boolean demo) {
        URI target=URI.create(url);
        if(!demo)throw new IllegalArgumentException("Enterprise evidence identity/ingestion is not configured");
        if(target.getUserInfo()!=null||target.getQuery()!=null||target.getFragment()!=null||target.getHost()==null||(!target.getPath().isEmpty()&&!target.getPath().equals("/")))throw new IllegalArgumentException("Invalid configured search endpoint");
        boolean local=java.util.Set.of("localhost","127.0.0.1","::1","evidence-search").contains(target.getHost());
        if(!target.getScheme().equals("https")&&!(target.getScheme().equals("http")&&local))throw new IllegalArgumentException("TLS is required outside the isolated local demo");
        if(username.isBlank()||password.isBlank())throw new IllegalArgumentException("Search credentials are required");
        String basic=Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8));
        return ApacheHttpClient5TransportBuilder.builder(new HttpHost(target.getScheme(),target.getHost(),target.getPort()))
            .setDefaultHeaders(new org.apache.hc.core5.http.Header[]{new BasicHeader("Authorization","Basic "+basic)})
            .setMapper(new JacksonJsonpMapper())
            .setAutomaticRetriesDisabled(true)
            .setRequestConfigCallback(c->c.setResponseTimeout(Timeout.ofSeconds(3)).setConnectionRequestTimeout(Timeout.ofSeconds(2)))
            .setConnectionConfigCallback(c->c.setConnectTimeout(Timeout.ofSeconds(2)).setSocketTimeout(Timeout.ofSeconds(3)))
            .build();
    }
    @Bean OpenSearchClient evidenceClient(OpenSearchTransport transport){return new OpenSearchClient(transport);}
}
