package com.investinator.connectors;
import com.investinator.health.*;
import static com.investinator.health.HealthCheckResult.*;
import org.apache.thrift.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import javax.security.auth.callback.*;
import javax.security.sasl.*;
import java.util.*;
/** Minimal fb303 getVersion and HMS get_database(default), with bounded Thrift decoding. */
public class HiveMetastoreConnector implements DiagnosticConnector {
    private final ConnectorConfig config;
    public HiveMetastoreConnector(ConnectorConfig config){this.config=config;}
    public String service(){return "hive-metastore";}
    public Descriptor descriptor(){return new Descriptor(service(),"SASL protected binary Thrift","HMS 3.x/4.x common read contract; fb303 server version required","GSSAPI auth-conf; DIGEST-MD5 only for explicit loopback fixture",Set.of(Capability.health,Capability.metadata,Capability.config,Capability.readOnlyChecks),List.of("Separate metastore metadata from HS2 query execution and backing RDBMS availability","No create/alter/drop, database listing or arbitrary Thrift method"));}
    protected TTransport open()throws Exception {
        var uri=java.net.URI.create(config.value("URL"));if(!"thrift".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException("HMS endpoint");
        var socket=new TSocket(new TConfiguration(1048576,1048576,32),uri.getHost(),uri.getPort()<0?9083:uri.getPort(),2000,2000);
        String mechanism=config.value("SASL_MECHANISM","GSSAPI");boolean local=Set.of("localhost","127.0.0.1","::1").contains(uri.getHost())&&config.flag("LOCAL_FIXTURE");
        if(!mechanism.equals("GSSAPI")&&!(local&&mechanism.equals("DIGEST-MD5")))throw new IllegalArgumentException("HMS authentication");
        CallbackHandler callbacks=values->{for(var callback:values){if(callback instanceof NameCallback n)n.setName(config.value("USER"));else if(callback instanceof PasswordCallback p)p.setPassword(config.value("PASSWORD").toCharArray());else if(callback instanceof RealmCallback r)r.setText(r.getDefaultText());else throw new UnsupportedCallbackException(callback);}};
        var transport=new TSaslClientTransport(mechanism,null,config.value("SERVICE_PRINCIPAL","hive"),uri.getHost(),Map.of(Sasl.QOP,"auth-conf",Sasl.SERVER_AUTH,"true"),mechanism.equals("GSSAPI")?null:callbacks,socket);try{transport.open();return transport;}catch(Exception e){transport.close();throw e;}
    }
    private static void call(TProtocol p,String name,boolean database)throws Exception {
        p.writeMessageBegin(new TMessage(name,TMessageType.CALL,1));p.writeStructBegin(new TStruct(name+"_args"));if(database){p.writeFieldBegin(new TField("name",TType.STRING,(short)1));p.writeString("default");p.writeFieldEnd();}p.writeFieldStop();p.writeStructEnd();p.writeMessageEnd();p.getTransport().flush();
    }
    private static String read(TProtocol p,String method,boolean database)throws Exception {
        var message=p.readMessageBegin();if(message.type==TMessageType.EXCEPTION)throw TApplicationException.readFrom(p);if(!method.equals(message.name)||message.seqid!=1)throw new TException("Contract mismatch");
        String answer=null;p.readStructBegin();for(TField f=p.readFieldBegin();f.type!=TType.STOP;f=p.readFieldBegin()) {
            if(f.id==0&&f.type==TType.STRING&&!database)answer=p.readString();
            else if(f.id==0&&f.type==TType.STRUCT&&database){p.readStructBegin();for(TField d=p.readFieldBegin();d.type!=TType.STOP;d=p.readFieldBegin()){if(d.id==1&&d.type==TType.STRING)answer=p.readString();else TProtocolUtil.skip(p,d.type);p.readFieldEnd();}p.readStructEnd();}
            else TProtocolUtil.skip(p,f.type);p.readFieldEnd();
        }p.readStructEnd();p.readMessageEnd();if(answer==null)throw new TException("Contract mismatch");return answer;
    }
    public HealthCheckResult check(){
        if(!config.configured())return ProbeCatalog.absent(service(),"Set CONNECTOR_HIVE_METASTORE_URL and backend SASL identity");
        if(config.value("SASL_MECHANISM","GSSAPI").equals("GSSAPI")&&System.getProperty("java.security.auth.login.config")==null)return ProbeCatalog.absent(service(),"Configure JVM JAAS Kerberos credentials for the HMS service principal");
        if(config.value("SASL_MECHANISM").equals("DIGEST-MD5")&&(config.value("USER").isBlank()||config.value("PASSWORD").isBlank()))return ProbeCatalog.absent(service(),"Configure isolated fixture SASL credentials");
        try(var transport=open()){var protocol=new TBinaryProtocol(transport);call(protocol,"getVersion",false);String version=read(protocol,"getVersion",false);if(!(version.startsWith("3.")||version.startsWith("4.")))return of(service(),capability(),Status.NOT_TESTED,SourceMode.LIVE,"authenticated HMS fb303 version","HMS did not expose a supported release version","Check this distribution's version contract before enabling metadata reads","UNSUPPORTED_VERSION",false,Map.of());call(protocol,"get_database",true);if(!"default".equals(read(protocol,"get_database",true)))throw new TException("Database contract");return ProbeCatalog.ok(service(),capability(),"Authenticated HMS version and default namespace metadata read passed",false,Map.of("server_version",version,"hiveserver2_status","NOT_TESTED","backing_database_status","NOT_TESTED"));}
        catch(Exception error){return ProbeCatalog.fail(service(),false,"Authenticated HMS read failed",error.getClass().getSimpleName());}
    }
}
