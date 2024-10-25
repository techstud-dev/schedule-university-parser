package com.techstud.sch_parser.handler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.techstud.sch_parser.kafka.KafkaConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringJUnitConfig
@EmbeddedKafka(partitions = 1, topics = {GlobalExceptionHandlerTest.ERROR_TOPIC})
public class GlobalExceptionHandlerTest {

    static final String ERROR_TOPIC = "error_topic";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHandleException_sendsErrorMessageToKafka() {
        Exception testException = new RuntimeException("Test exception");

        String testMessageId = "test-message-id";
        KafkaConsumer.messageIdHolder.set(testMessageId);

        globalExceptionHandler.handleException(testException);

        Map<String, String> expectedMessage = new LinkedHashMap<>();
        expectedMessage.put("systemName", "MySystem");
        expectedMessage.put("serviceName", "MyService");
        expectedMessage.put("messageId", testMessageId);
        expectedMessage.put("message", testException.getMessage());

        verify(kafkaTemplate, times(1)).send(eq(ERROR_TOPIC), eq(expectedMessage.toString()));

        KafkaConsumer.messageIdHolder.remove();
    }
}
