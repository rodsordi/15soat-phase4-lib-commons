package br.com.fiap.phase4.commons.kafka.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventEnvelope<T>(
        EventMetadata metadata,
        T payload
) implements Serializable {

    public EventEnvelope {
        Objects.requireNonNull(metadata, "Event metadata must not be null");
        Objects.requireNonNull(payload, "Event payload must not be null");
    }

    public static <T> EventEnvelope<T> of(String eventType, String sourceService, String traceId, T payload) {
        EventMetadata metadata = EventMetadata.of(eventType, sourceService, traceId);
        return new EventEnvelope<>(metadata, payload);
    }
}
