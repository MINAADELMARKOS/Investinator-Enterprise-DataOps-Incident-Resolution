package com.investinator.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
@Testcontainers
class MigrationOwnershipTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e").asCompatibleSubstituteFor("postgres"));
    @Test void existingAlembicSchemaIsNeverAdoptedOrModified() throws Exception {
        try(var connection=DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());var sql=connection.createStatement()) {
            sql.execute("create schema legacy_test");sql.execute("create table legacy_test.alembic_version(version_num varchar(32))");
            sql.execute("insert into legacy_test.alembic_version values ('bf50bd87cb88')");
            assertThrows(IllegalStateException.class,()->new PersistenceConfiguration().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword(),"legacy_test"));
            try(var row=sql.executeQuery("select version_num from legacy_test.alembic_version")){assertTrue(row.next());assertEquals("bf50bd87cb88",row.getString(1));}
            try(var row=sql.executeQuery("select count(*) from information_schema.tables where table_schema='legacy_test'")){assertTrue(row.next());assertEquals(1,row.getInt(1));}
        }
    }
    @Test void publicAndSystemSchemasAreRefusedBeforeAnyMigration() {
        for(String schema:java.util.List.of("public","pg_catalog","information_schema","bad;drop"))
            assertThrows(IllegalArgumentException.class,()->new PersistenceConfiguration().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword(),schema));
    }
}
