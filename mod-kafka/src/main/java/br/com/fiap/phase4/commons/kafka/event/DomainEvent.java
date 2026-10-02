package br.com.fiap.phase4.commons.kafka.event;

import java.time.Instant;

public interface DomainEvent {

    String eventId();

    String eventType();

    Instant occurredOn();
}
