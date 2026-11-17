package com.investinator.knowledge;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.mock.env.MockEnvironment;
class KnowledgeReviewIdentityTest {
    @Test void ingestionAndFeedbackCredentialsCannotDoubleAsIndependentReviewIdentity(){for(String producer:new String[]{"KNOWLEDGE_WRITE_TOKEN","KNOWLEDGE_FEEDBACK_TOKEN"}){var env=new MockEnvironment().withProperty(producer,"same-fixture-key").withProperty("KNOWLEDGE_REVIEW_TOKEN","same-fixture-key");assertThrows(IllegalArgumentException.class,()->new KnowledgeService(null,null,null,null,env));}}
}
