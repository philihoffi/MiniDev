package org.philipp.fun.minidev.intent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntentRequestTest {

    @Test
    void constructorStoresAllProvidedValues() {
        IntentRequest.IntentType intent = IntentRequest.IntentType.START_WORK;
        UUID runId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-04-10T10:15:30Z");

        IntentRequest request = new IntentRequest(intent, runId, requestedAt);

        assertAll(
                () -> assertEquals(intent, request.intent()),
                () -> assertEquals(runId, request.runId()),
                () -> assertEquals(requestedAt, request.requestedAt())
        );
    }

    @Test
    void convenienceConstructorGeneratesTimestamp() {
        IntentRequest.IntentType intent = IntentRequest.IntentType.CONTINUE_WORK;
        UUID runId = UUID.randomUUID();

        IntentRequest request = new IntentRequest(intent, runId);

        assertAll(
                () -> assertEquals(intent, request.intent()),
                () -> assertEquals(runId, request.runId()),
                () -> assertNotNull(request.requestedAt())
        );
    }

    @Test
    void recordsAreEqualWhenAllFieldsMatch() {
        IntentRequest.IntentType intent = IntentRequest.IntentType.IMPROVE_CURRENT_GAME;
        UUID runId = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-04-10T10:15:30Z");

        IntentRequest request1 = new IntentRequest(intent, runId, requestedAt);
        IntentRequest request2 = new IntentRequest(intent, runId, requestedAt);

        assertAll(
                () -> assertEquals(request1, request2),
                () -> assertEquals(request1.hashCode(), request2.hashCode())
        );
    }

    @Test
    void recordsAreNotEqualWhenFieldsDiffer() {
        IntentRequest.IntentType intent = IntentRequest.IntentType.START_WORK;
        UUID runId1 = UUID.randomUUID();
        UUID runId2 = UUID.randomUUID();
        Instant requestedAt = Instant.parse("2026-04-10T10:15:30Z");

        IntentRequest request1 = new IntentRequest(intent, runId1, requestedAt);
        IntentRequest request2 = new IntentRequest(intent, runId2, requestedAt);

        assertNotEquals(request1, request2);
    }
}
