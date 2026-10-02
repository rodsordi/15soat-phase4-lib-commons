package br.com.fiap.phase4.commons.kafka.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record EventMetadata(
        String eventId,
        String eventType,
        String traceId,
        String spanId,
        String sourceService,
        Instant timestamp
) implements Serializable {

    public static EventMetadata of(String eventType, String sourceService, String traceId) {
        String eventId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new EventMetadata(eventId, eventType, traceId, spanId, sourceService, Instant.now());
    }
}
