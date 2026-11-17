package com.investinator.connectors;
import com.investinator.health.*;
import static com.investinator.health.HealthCheckResult.*;
import java.sql.*;
import java.util.*;
/** Fixed SELECT 1; drivers are an explicit optional Maven profile, not downloaded at runtime. */
public class HiveServer2Connector implements DiagnosticConnector {
    private final ConnectorConfig config;
    public HiveServer2Connector(ConnectorConfig config){this.config=config;}
    public String service(){return "hive";}
    public Descriptor descriptor(){return new Descriptor(service(),"HiveServer2 JDBC/Thrift","Hive JDBC driver 4.2.1; server product 3.x/4.x checked via metadata","Kerberos principal or TLS + scoped backend username/password",Set.of(Capability.health,Capability.metadata,Capability.config,Capability.readOnlyChecks),List.of("Separate from metastore health","Requires explicit hive-jdbc Maven profile","No user SQL; only SELECT 1; metastore/backend table queries are not inferred"));}
    protected Connection open()throws Exception {Class.forName("org.apache.hive.jdbc.HiveDriver");var props=new Properties();props.setProperty("user",config.value("USER"));props.setProperty("password",config.value("PASSWORD"));props.setProperty("loginTimeout","2");props.setProperty("socketTimeout","3");String url=config.value("JDBC_URL");String timeout=url.contains(";transportMode=http")?"2":"2000";return DriverManager.getConnection(url+";thrift.client.max.message.size=1048576;socketTimeout="+timeout,props);}
    public HealthCheckResult check() {
        String url=config.value("JDBC_URL");if(url.isBlank())return ProbeCatalog.absent(service(),"Set CONNECTOR_HIVE_JDBC_URL and read-only authentication; HMS uses CONNECTOR_HIVE_METASTORE_*");
        var parameters=new HashMap<String,String>();
        try{for(String item:url.split(";",-1)){if(!item.contains("="))continue;String[] pair=item.split("=",2);if(parameters.putIfAbsent(pair[0],pair[1])!=null)throw new IllegalArgumentException();if(!Set.of("ssl","sslTrustStore","trustStorePassword","transportMode","httpPath","principal","saslQop").contains(pair[0]))throw new IllegalArgumentException();}}catch(IllegalArgumentException e){return ProbeCatalog.fail(service(),false,"Unsupported or duplicate JDBC option","UNSAFE_JDBC_CONFIGURATION");}
        if(parameters.containsKey("transportMode")&&!Set.of("http","binary").contains(parameters.get("transportMode")))return ProbeCatalog.fail(service(),false,"Unsupported JDBC transport mode","UNSAFE_JDBC_CONFIGURATION");
        boolean kerberos=url.contains(";principal=")&&System.getProperty("java.security.auth.login.config")!=null;
        if(!url.startsWith("jdbc:hive2://")||url.contains("\n")||url.toLowerCase(Locale.ROOT).contains("initfile")||url.contains("#")||url.contains("?"))return ProbeCatalog.fail(service(),false,"Only bounded HiveServer2 connection configuration is supported","UNSAFE_JDBC_CONFIGURATION");
        if(kerberos&&!"true".equals(parameters.get("ssl"))&&!"auth-conf".equals(parameters.get("saslQop")))return ProbeCatalog.absent(service(),"Require encrypted Kerberos QOP auth-conf or TLS");
        if(!kerberos&&(!"true".equals(parameters.get("ssl"))||config.value("USER").isBlank()||config.value("PASSWORD").isBlank()))return ProbeCatalog.absent(service(),"Require Kerberos or verified TLS plus scoped backend credentials");
        try(var connection=open()) {
            String version=connection.getMetaData().getDatabaseProductVersion();if(version==null||!(version.startsWith("3.")||version.startsWith("4.")))return ProbeCatalog.fail(service(),false,"Hive server version is unsupported","UNSUPPORTED_VERSION");
            try{connection.setReadOnly(true);}catch(SQLFeatureNotSupportedException ignored){}
            try(var query=connection.createStatement()){query.setQueryTimeout(3);query.setMaxRows(1);try(var rows=query.executeQuery("SELECT 1")){if(!rows.next()||rows.getInt(1)!=1)return ProbeCatalog.fail(service(),false,"Hive safe query failed","QUERY_FAILED");}}
            return ProbeCatalog.ok(service(),capability(),"Authenticated HiveServer2 metadata and fixed bounded SELECT 1 passed",false,Map.of("server_version",version,"metastore_backend_status","NOT_TESTED"));
        }catch(ClassNotFoundException e){return of(service(),capability(),Status.NOT_TESTED,SourceMode.NOT_TESTED,"backend driver configuration","Hive JDBC profile is not installed","Build with -Phive-jdbc; validate scoped private-cluster credentials independently","DRIVER_NOT_INSTALLED",false,Map.of());}
        catch(Exception e){return ProbeCatalog.fail(service(),false,"Authenticated HiveServer2 read failed",e.getClass().getSimpleName());}
    }
}
