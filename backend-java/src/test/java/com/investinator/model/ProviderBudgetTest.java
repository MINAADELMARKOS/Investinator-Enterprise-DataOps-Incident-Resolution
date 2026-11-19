package com.investinator.model;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
@Testcontainers
class ProviderBudgetTest {
    @Container static final PostgreSQLContainer pg=new PostgreSQLContainer(org.testcontainers.utility.DockerImageName.parse("postgres:16-alpine@sha256:81e77a4833487b8132fe4b00bedd39dc3f74b8641e387f19312a48f7de218c2e").asCompatibleSubstituteFor("postgres"));
    JdbcTemplate jdbc;ProviderBudget ledger;MockEnvironment env;
    @BeforeEach void init()throws Exception{
        jdbc=new JdbcTemplate(new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword()));
        jdbc.execute("drop schema if exists budget_fixture cascade;create schema budget_fixture");
        String url=pg.getJdbcUrl()+(pg.getJdbcUrl().contains("?")?"&":"?")+"currentSchema=budget_fixture";
        jdbc=new JdbcTemplate(new DriverManagerDataSource(url,pg.getUsername(),pg.getPassword()));
        jdbc.execute("create table audit_events(tenant_id text,payload jsonb,created_at timestamptz,id text,event text)");jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V9__durable_provider_budget.sql")));
        env=new MockEnvironment().withProperty("INVESTINATOR_OPENAI_PAID_OPT_IN","true").withProperty("INVESTINATOR_OPENAI_COST_APPROVED","true").withProperty("INVESTINATOR_OPENAI_APPROVAL_REF","SIMULATED-COST-APPROVAL");ledger=new ProviderBudget(jdbc,env);
    }
    void configure(long global,long tenant){jdbc.update("insert into provider_budget_prices values('fixture-model',1000000,2000000,'SIMULATED-COST-APPROVAL',now()+interval '1 hour')");jdbc.update("insert into provider_budget_accounts(tenant_id,model,limit_microusd,approval_ref,approved_until) values('*','*',?,'SIMULATED-COST-APPROVAL',now()+interval '1 hour'),('demo','fixture-model',?,'SIMULATED-COST-APPROVAL',now()+interval '1 hour')",global,tenant);}
    @Test void noPriceQuotaOrOptInDeniesWithoutProviderCalls(){assertThrows(Exception.class,()->ledger.reserve("demo","fixture-model",100,64));configure(10000,10000);env.setProperty("INVESTINATOR_OPENAI_PAID_OPT_IN","false");assertThrows(Exception.class,()->ledger.reserve("demo","fixture-model",100,64));assertEquals(0,jdbc.queryForObject("select count(*) from provider_budget_reservations",Integer.class));}
    @Test void parallelReservationsCannotExceedGlobalOrTenantQuota()throws Exception{configure(228,10000);try(var pool=Executors.newFixedThreadPool(4)){var tasks=new ArrayList<Future<Boolean>>();for(int i=0;i<12;i++)tasks.add(pool.submit(()->{try{ledger.reserve("demo","fixture-model",100,64);return true;}catch(Exception denied){return false;}}));int admitted=0;for(var task:tasks)if(task.get())admitted++;assertEquals(1,admitted);}assertEquals(228L,jdbc.queryForObject("select reserved_microusd from provider_budget_accounts where tenant_id='*'",Long.class));}
    @Test void unknownTimeoutHoldSurvivesServiceRestartAndReconciliationIsImmutable(){configure(10000,10000);var r=ledger.reserve("demo","fixture-model",100,64);ledger.reconcile("demo",r.id(),null,null);ledger=new ProviderBudget(jdbc,env);assertEquals("UNKNOWN",jdbc.queryForObject("select state from provider_budget_reservations",String.class));assertEquals(228L,jdbc.queryForObject("select reserved_microusd from provider_budget_accounts where tenant_id='*'",Long.class));ledger.reconcile("demo",r.id(),10L,5L);ledger.reconcile("demo",r.id(),10L,5L);assertEquals(20L,jdbc.queryForObject("select charged_microusd from provider_budget_accounts where tenant_id='*'",Long.class));assertEquals(0L,jdbc.queryForObject("select reserved_microusd from provider_budget_accounts where tenant_id='*'",Long.class));assertThrows(Exception.class,()->ledger.reconcile("other",r.id(),10L,5L));assertThrows(Exception.class,()->ledger.reconcile("demo",r.id(),11L,5L));}
    @Test void expiredApprovalUnknownModelAndTenantQuotaFailClosed(){configure(10000,227);assertThrows(Exception.class,()->ledger.reserve("demo","fixture-model",100,64));assertThrows(Exception.class,()->ledger.reserve("other","fixture-model",1,1));assertThrows(Exception.class,()->ledger.reserve("demo","unknown",1,1));jdbc.update("update provider_budget_accounts set approved_until=now()-interval '1 second'");assertThrows(Exception.class,()->ledger.reserve("demo","fixture-model",1,1));}
    @Test void reportedOverrunChargesActualAndPreventsFurtherSpend(){configure(228,228);var r=ledger.reserve("demo","fixture-model",100,64);ledger.reconcile("demo",r.id(),1000L,1000L);assertEquals("OVERDRAWN",jdbc.queryForObject("select state from provider_budget_reservations",String.class));assertEquals(3000L,jdbc.queryForObject("select charged_microusd from provider_budget_accounts where tenant_id='*'",Long.class));assertThrows(Exception.class,()->ledger.reserve("demo","fixture-model",1,1));}
}
