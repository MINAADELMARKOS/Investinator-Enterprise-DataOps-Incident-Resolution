package com.investinator.connectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import com.investinator.health.*;
import java.sql.*;
import java.util.*;
import org.apache.kudu.client.*;
class NativeConnectorTest {
    @Test void hiveOnlyExecutesOneFixedBoundedReadAndClosesResources()throws Exception {
        var connection=mock(Connection.class);var meta=mock(DatabaseMetaData.class);var statement=mock(Statement.class);var rows=mock(ResultSet.class);when(connection.getMetaData()).thenReturn(meta);when(meta.getDatabaseProductVersion()).thenReturn("4.2.1");when(connection.createStatement()).thenReturn(statement);when(statement.executeQuery("SELECT 1")).thenReturn(rows);when(rows.next()).thenReturn(true);when(rows.getInt(1)).thenReturn(1);
        var env=new MockEnvironment().withProperty("CONNECTOR_HIVE_JDBC_URL","jdbc:hive2://private.example:10000/default;ssl=true").withProperty("CONNECTOR_HIVE_USER","reader").withProperty("CONNECTOR_HIVE_PASSWORD","fixture-password");
        var connector=new HiveServer2Connector(new ConnectorConfig(env,"hive")){protected Connection open(){return connection;}};assertEquals(HealthCheckResult.Status.HEALTHY,connector.check().status());
        verify(statement).setMaxRows(1);verify(statement).setQueryTimeout(3);verify(statement).executeQuery("SELECT 1");verify(statement).close();verifyNoMoreInteractions(statement);verify(connection).setReadOnly(true);verify(connection).close();
        when(meta.getDatabaseProductVersion()).thenReturn("99.0");assertEquals("UNSUPPORTED_VERSION",connector.check().error_type());
    }
    @Test void hiveUnsafeTransportDuplicateTlsAndUninstalledDriverAreExplicit(){
        var env=new MockEnvironment().withProperty("CONNECTOR_HIVE_JDBC_URL","jdbc:hive2://host/default;ssl=true;ssl=false").withProperty("CONNECTOR_HIVE_USER","reader").withProperty("CONNECTOR_HIVE_PASSWORD","fixture-password");
        assertEquals("UNSAFE_JDBC_CONFIGURATION",new HiveServer2Connector(new ConnectorConfig(env,"hive")).check().error_type());
        env.setProperty("CONNECTOR_HIVE_JDBC_URL","jdbc:hive2://host/default;ssl=true");assertEquals("DRIVER_NOT_INSTALLED",new HiveServer2Connector(new ConnectorConfig(env,"hive")){protected Connection open()throws Exception{throw new ClassNotFoundException("Simulated missing optional driver");}}.check().error_type());
    }
    @Test void kuduUsesOnlyOfficialReadRpcsAndDoesNotInventQuorumOrServerVersion()throws Exception {
        var client=mock(KuduClient.class);var servers=mock(ListTabletServersResponse.class);when(servers.getTabletServersCount()).thenReturn(2);when(client.listTabletServers()).thenReturn(servers);when(client.findLeaderMasterServer()).thenReturn(new HostAndPort("fixture",7051));
        var env=new MockEnvironment().withProperty("CONNECTOR_KUDU_MASTERS","fixture:7051").withProperty("CONNECTOR_KUDU_JWT","test-only");
        var connector=new KuduDiagnosticConnector(new ConnectorConfig(env,"kudu")){protected KuduClient open(){return client;}};var report=connector.check();assertEquals(HealthCheckResult.Status.NOT_TESTED,report.status());assertEquals("NOT_TESTED",report.details().get("server_version"));
        verify(client).listTabletServers();verify(client).findLeaderMasterServer();verify(client).close();verifyNoMoreInteractions(client);
        when(servers.getTabletServersCount()).thenReturn(0);assertEquals(HealthCheckResult.Status.DEGRADED,connector.check().status());
    }
    @Test void nativePrivateServicesWithoutCredentialsAreNotConfigured(){
        for(DiagnosticConnector connector:List.of(new KuduDiagnosticConnector(new ConnectorConfig(new MockEnvironment(),"kudu")),new HiveServer2Connector(new ConnectorConfig(new MockEnvironment(),"hive")),new HiveMetastoreConnector(new ConnectorConfig(new MockEnvironment(),"hive-metastore")))){try{assertEquals(HealthCheckResult.Status.NOT_CONFIGURED,connector.check().status());}catch(Exception e){fail(e);}}
    }
}
