package br.com.fiap.phase4.commons.kafka;

import br.com.fiap.phase4.commons.kafka.event.EventEnvelope;
import br.com.fiap.phase4.commons.kafka.tracing.KafkaTraceContextUtils;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kafka Commons Tests")
class KafkaCommonsTest {

    @Nested
    @DisplayName("EventEnvelope Tests")
    class EventEnvelopeTests {

        @Test
        @DisplayName("Should create event envelope with metadata and payload")
        void shouldCreateEnvelope() {
            var payload = Map.of("orderId", "123", "status", "APPROVED");
            var envelope = EventEnvelope.of("WorkOrderApprovedEvent", "api-work-order", "trace-123", payload);

            assertThat(envelope.metadata().eventType()).isEqualTo("WorkOrderApprovedEvent");
            assertThat(envelope.metadata().sourceService()).isEqualTo("api-work-order");
            assertThat(envelope.metadata().traceId()).isEqualTo("trace-123");
            assertThat(envelope.payload()).containsEntry("orderId", "123");
        }
    }

    @Nested
    @DisplayName("KafkaTraceContextUtils Tests")
    class TracingTests {

        @Test
        @DisplayName("Should inject and extract W3C traceparent and traceId in Kafka headers")
        void shouldInjectAndExtractTrace() {
            var headers = new RecordHeaders();
            String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
            String spanId = "00f067aa0ba902b7";

            KafkaTraceContextUtils.injectTraceContext(headers, traceId, spanId);

            String extracted = KafkaTraceContextUtils.extractTraceId(headers);
            assertThat(extracted).isEqualTo(traceId);
        }
    }
}
