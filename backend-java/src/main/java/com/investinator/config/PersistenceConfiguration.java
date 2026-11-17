package com.investinator.config;
import javax.sql.DataSource;
import java.sql.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@Configuration
@ConditionalOnProperty(name="investinator.persistence.enabled",havingValue="true")
public class PersistenceConfiguration {
    @Bean(destroyMethod="close")
    HikariDataSource dataSource(@Value("${investinator.persistence.url}") String url,
        @Value("${investinator.persistence.username}") String user,@Value("${investinator.persistence.password}") String password,
        @Value("${investinator.persistence.schema:investinator_java}") String schema) throws Exception {
        if (!schema.matches("[a-z][a-z0-9_]{0,62}") || schema.equals("public") || schema.startsWith("pg_") || schema.equals("information_schema"))
            throw new IllegalArgumentException("Java requires a dedicated non-public schema");
        if (!url.startsWith("jdbc:postgresql:")) throw new IllegalArgumentException("PostgreSQL is required");
        var config=new HikariConfig();config.setJdbcUrl(url);config.setUsername(user);config.setPassword(password);
        config.setMaximumPoolSize(5);config.setConnectionTimeout(5000);config.setSchema(schema);
        config.addDataSourceProperty("connectTimeout","3");config.addDataSourceProperty("socketTimeout","5");
        var pool=new HikariDataSource(config);
        try {
            try (var connection=pool.getConnection();var query=connection.prepareStatement(
                "select table_name from information_schema.tables where table_schema=? and table_type='BASE TABLE'")) {
                query.setString(1,schema);var names=new java.util.HashSet<String>();
                try(var rows=query.executeQuery()){while(rows.next())names.add(rows.getString(1));}
                if (!names.isEmpty() && !names.contains("flyway_schema_history"))
                    throw new IllegalStateException("Refusing to adopt an existing schema; restore or migrate separately");
                if (names.contains("alembic_version")) throw new IllegalStateException("Alembic owns this schema");
            }
            Flyway.configure().dataSource(pool).schemas(schema).defaultSchema(schema).createSchemas(true)
                .baselineOnMigrate(false).cleanDisabled(true).validateMigrationNaming(true).load().migrate();
            return pool;
        } catch(Exception error) {pool.close();throw error;}
    }
    @Bean JdbcTemplate jdbcTemplate(DataSource source) {var jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(5);return jdbc;}
    @Bean TransactionTemplate transactionTemplate(DataSource source) {var tx=new TransactionTemplate(new JdbcTransactionManager(source));tx.setTimeout(15);return tx;}
}
