package com.investinator.connectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.env.MockEnvironment;
import com.investinator.health.HealthCheckResult;
import org.apache.thrift.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.server.*;
import javax.security.auth.callback.*;
import javax.security.sasl.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
class HiveMetastoreConnectorTest {
    @Test void actualDigestSaslThriftFixtureAllowsOnlyVersionAndNamespaceReads()throws Exception {
        var methods=new CopyOnWriteArrayList<String>();var version=new AtomicReference<>("4.2.1");
        TProcessor processor=(in,out)->{
            var message=in.readMessageBegin();methods.add(message.name);in.readStructBegin();for(var f=in.readFieldBegin();f.type!=TType.STOP;f=in.readFieldBegin()){if(message.name.equals("get_database")&&f.id==1)assertEquals("default",in.readString());else TProtocolUtil.skip(in,f.type);in.readFieldEnd();}in.readStructEnd();in.readMessageEnd();
            if(!Set.of("getVersion","get_database").contains(message.name))throw new TException("Write method rejected");
            out.writeMessageBegin(new TMessage(message.name,TMessageType.REPLY,message.seqid));out.writeStructBegin(new TStruct("result"));
            if(message.name.equals("getVersion")){out.writeFieldBegin(new TField("success",TType.STRING,(short)0));out.writeString(version.get());out.writeFieldEnd();}
            else{out.writeFieldBegin(new TField("success",TType.STRUCT,(short)0));out.writeStructBegin(new TStruct("Database"));out.writeFieldBegin(new TField("name",TType.STRING,(short)1));out.writeString("default");out.writeFieldEnd();out.writeFieldStop();out.writeStructEnd();out.writeFieldEnd();}
            out.writeFieldStop();out.writeStructEnd();out.writeMessageEnd();out.getTransport().flush();
        };
        CallbackHandler callback=callbacks->{for(var c:callbacks){if(c instanceof NameCallback n)n.setName("reader");else if(c instanceof PasswordCallback p)p.setPassword("fixture-only".toCharArray());else if(c instanceof RealmCallback r)r.setText(r.getDefaultText());else if(c instanceof AuthorizeCallback a)a.setAuthorized(a.getAuthenticationID().equals("reader")&&a.getAuthorizationID().equals("reader"));else throw new UnsupportedCallbackException(c);}};
        var transportFactory=new TSaslServerTransport.Factory("DIGEST-MD5","hive","127.0.0.1",Map.of(Sasl.QOP,"auth-conf"),callback);
        var socket=new TServerSocket(new java.net.InetSocketAddress("127.0.0.1",0));var server=new TThreadPoolServer(new TThreadPoolServer.Args(socket).processor(processor).transportFactory(transportFactory).protocolFactory(new TBinaryProtocol.Factory()).minWorkerThreads(1).maxWorkerThreads(2));
        var thread=Thread.ofPlatform().daemon(true).start(server::serve);
        try {
            var env=new MockEnvironment().withProperty("CONNECTOR_HIVE_METASTORE_URL","thrift://127.0.0.1:"+socket.getServerSocket().getLocalPort()).withProperty("CONNECTOR_HIVE_METASTORE_SASL_MECHANISM","DIGEST-MD5").withProperty("CONNECTOR_HIVE_METASTORE_LOCAL_FIXTURE","true").withProperty("CONNECTOR_HIVE_METASTORE_USER","reader").withProperty("CONNECTOR_HIVE_METASTORE_PASSWORD","fixture-only");
            var connector=new HiveMetastoreConnector(new ConnectorConfig(env,"hive-metastore"));var report=connector.check();assertEquals(HealthCheckResult.Status.HEALTHY,report.status(),report.error_type());assertEquals(List.of("getVersion","get_database"),methods);
            version.set("2.3.0");assertEquals("UNSUPPORTED_VERSION",connector.check().error_type());assertEquals(3,methods.size());
            env.setProperty("CONNECTOR_HIVE_METASTORE_PASSWORD","wrong-secret-never-echo");var denied=connector.check();assertEquals(HealthCheckResult.Status.UNHEALTHY,denied.status());assertFalse(denied.toString().contains("wrong-secret"));assertTrue(methods.stream().allMatch(Set.of("getVersion","get_database")::contains));
        }finally{server.stop();socket.close();thread.join(3000);}
    }
}
