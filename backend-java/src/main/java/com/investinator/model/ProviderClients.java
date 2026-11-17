package com.investinator.model;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.core.env.Environment;
import java.net.URI;
import java.time.Duration;
import java.util.*;
public final class ProviderClients {
    private ProviderClients(){}
    public static OpenAIClient create(Environment env) {
        String url=env.getProperty("INVESTINATOR_OPENAI_BASE_URL","https://api.openai.com/v1");URI target=URI.create(url);
        boolean fixture=env.getProperty("INVESTINATOR_OPENAI_TEST_ENDPOINT",Boolean.class,false)&&Set.of("127.0.0.1","localhost").contains(Objects.toString(target.getHost(),""));
        var hosts=Set.copyOf(Arrays.asList(env.getProperty("INVESTINATOR_OPENAI_ALLOWED_HOSTS","api.openai.com").split(",")));
        if(target.getUserInfo()!=null||target.getQuery()!=null||target.getFragment()!=null||!((target.getScheme().equals("https")&&hosts.contains(target.getHost()))||(fixture&&target.getScheme().equals("http"))))throw new IllegalArgumentException("Untrusted provider endpoint");
        var builder=OpenAIOkHttpClient.builder().apiKey(env.getRequiredProperty("OPENAI_API_KEY")).baseUrl(url).timeout(Duration.ofSeconds(8)).maxRetries(0);
        String proxy=env.getProperty("HTTPS_PROXY",env.getProperty("https_proxy",""));if(!fixture&&!proxy.isBlank()){URI address=URI.create(proxy);if(address.getUserInfo()!=null||address.getHost()==null)throw new IllegalArgumentException("Use a separately authenticated secure proxy configuration");builder.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP,new java.net.InetSocketAddress(address.getHost(),address.getPort()<0?80:address.getPort())));}
        return builder.build();
    }
}
