package com.investinator.connectors;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.env.Environment;
/** All values originate in trusted deployment configuration, never request/model arguments. */
public class ConnectorConfig {
    private final Environment env;public final String service,prefix;
    public ConnectorConfig(Environment env,String service){this.env=env;this.service=service;prefix="CONNECTOR_"+service.toUpperCase(Locale.ROOT).replace('-','_')+"_";}
    public String value(String key){return env.getProperty(prefix+key,"");}
    public String value(String key,String fallback){String v=value(key);return v.isBlank()?fallback:v;}
    public boolean flag(String key){return Boolean.parseBoolean(value(key));}
    public int limit(String key,int fallback,int max){int n=Integer.parseInt(value(key,Integer.toString(fallback)));if(n<1||n>max)throw new IllegalArgumentException("Invalid connector bound");return n;}
    public int threshold(String key,int fallback,int max){int n=Integer.parseInt(value(key,Integer.toString(fallback)));if(n<0||n>max)throw new IllegalArgumentException("Invalid connector threshold");return n;}
    public boolean configured(){return !value("URL").isBlank();}
    public URI endpoint(){URI uri=URI.create(value("URL"));if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("Invalid connector endpoint");boolean local=Set.of("127.0.0.1","localhost","::1").contains(uri.getHost());if(!"https".equals(uri.getScheme())&&!(local&&flag("LOCAL_FIXTURE")&&"http".equals(uri.getScheme())))throw new IllegalArgumentException("Verified TLS required outside explicit loopback fixture");return uri;}
    public String auth(){return value("AUTH","BEARER").toUpperCase(Locale.ROOT);}
    public boolean authenticationConfigured(){return switch(auth()){case "BEARER" -> !value("TOKEN").isBlank();case "BASIC" -> !value("USER").isBlank()&&!value("PASSWORD").isBlank();case "MTLS" -> System.getProperty("javax.net.ssl.keyStore")!=null;case "SPNEGO" -> System.getProperty("java.security.auth.login.config")!=null;default -> false;};}
    public Map<String,String> headers(){return switch(auth()){case "BEARER" -> Map.of("Authorization","Bearer "+value("TOKEN"));case "BASIC" -> Map.of("Authorization","Basic "+Base64.getEncoder().encodeToString((value("USER")+":"+value("PASSWORD")).getBytes(StandardCharsets.UTF_8)));default -> Map.of();};}
}
