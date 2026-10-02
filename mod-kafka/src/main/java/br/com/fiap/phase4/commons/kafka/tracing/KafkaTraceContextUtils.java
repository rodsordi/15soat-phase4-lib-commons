package br.com.fiap.phase4.commons.kafka.tracing;

import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class KafkaTraceContextUtils {

    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private KafkaTraceContextUtils() {
    }

    public static void injectTraceContext(Headers headers, String traceId, String spanId) {
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        if (spanId == null || spanId.isBlank()) {
            spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        String traceparent = String.format("00-%s-%s-01", traceId, spanId);
        headers.remove(TRACEPARENT_HEADER);
        headers.add(TRACEPARENT_HEADER, traceparent.getBytes(StandardCharsets.UTF_8));

        headers.remove(TRACE_ID_HEADER);
        headers.add(TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
    }

    public static String extractTraceId(Headers headers) {
        var traceparentHeader = headers.lastHeader(TRACEPARENT_HEADER);
        if (traceparentHeader != null && traceparentHeader.value() != null) {
            String traceparent = new String(traceparentHeader.value(), StandardCharsets.UTF_8);
            if (traceparent.startsWith("00-")) {
                String[] parts = traceparent.split("-");
                if (parts.length >= 2 && parts[1].length() == 32) {
                    return parts[1];
                }
            }
        }
        var traceIdHeader = headers.lastHeader(TRACE_ID_HEADER);
        if (traceIdHeader != null && traceIdHeader.value() != null) {
            return new String(traceIdHeader.value(), StandardCharsets.UTF_8);
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
