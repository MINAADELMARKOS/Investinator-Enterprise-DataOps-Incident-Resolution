package com.investinator.knowledge;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.env.MockEnvironment;
import com.investinator.persistence.Store;
import java.util.*;
class KnowledgeUnavailableTest {
    @Test void configuredButUnavailableIndexCannotClaimLiveRetrievalOrInventHits(){var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject(anyString(),eq(Long.class),eq("demo"))).thenReturn(0L);var index=mock(KnowledgeIndex.class);when(index.configured()).thenReturn(true);when(index.search("demo","demo.senior_sre","network","Kafka","4","KRAFT")).thenReturn(new KnowledgeIndex.Retrieval("UNAVAILABLE",List.of(),true));var service=new KnowledgeService(new Store(jdbc,new TransactionTemplate()),null,null,index,new MockEnvironment());var result=service.search("demo","demo.senior_sre","network",new KnowledgeService.SearchScope("Kafka","4","KRAFT"));assertEquals("UNAVAILABLE",result.get("status"));assertEquals("NOT_TESTED",result.get("retrieval_mode"));assertEquals(List.of(),result.get("items"));assertEquals(true,result.get("partial"));}
}
