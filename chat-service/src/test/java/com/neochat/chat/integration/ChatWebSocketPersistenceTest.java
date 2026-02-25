package com.neochat.chat.integration;

import com.neochat.chat.model.Conversation;
import com.neochat.chat.repository.ConversationRepository;
import com.neochat.common.storage.EventStore;
import com.neochat.common.storage.MongoEventStore;
import com.neochat.common.storage.PostgresEventStore;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ChatWebSocketPersistenceTest {

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    MongoEventStore mongoEventStore;

    @Inject
    PostgresEventStore postgresEventStore;

    private final ChatWebSocketIntegrationRunner runner = new ChatWebSocketIntegrationRunner();

    @Test
    void shouldSendMessageWithoutOidcFlowAndPersistToBothStores() {
        ChatWebSocketIntegrationRunner.Persona learner = new ChatWebSocketIntegrationRunner.Persona(
                "learner-1",
                "learner-1@neochat.local",
                Set.of("LEARNER"),
                Set.of("cohort-a"));

        String conversationId = createConversation(Set.of(learner.subject()), Set.of());
        String content = "integration-message-" + UUID.randomUUID();

        runner.sendMessage(baseUri, conversationId, learner, content);

        assertPersistedInBothStores(conversationId, learner.subject(), content);
    }

    @Test
    void shouldSupportPersonaScenariosWithDifferentRoles() {
        ChatWebSocketIntegrationRunner.Persona tutor = new ChatWebSocketIntegrationRunner.Persona(
                "tutor-1",
                "tutor-1@neochat.local",
                Set.of("TUTOR", "MODERATOR"),
                Set.of("staff"));

        ChatWebSocketIntegrationRunner.Persona observer = new ChatWebSocketIntegrationRunner.Persona(
                "observer-1",
                "observer-1@neochat.local",
                Set.of("OBSERVER"),
                Set.of("auditors"));

        String conversationId = createConversation(
                Set.of(tutor.subject()),
                Set.of(observer.subject()));

        String tutorContent = "persona-tutor-message-" + UUID.randomUUID();
        String observerContent = "persona-observer-message-" + UUID.randomUUID();

        runner.sendMessage(baseUri, conversationId, tutor, tutorContent);
        runner.sendMessage(baseUri, conversationId, observer, observerContent);

        assertPersistedInBothStores(conversationId, tutor.subject(), tutorContent);
        assertPersistedInBothStores(conversationId, observer.subject(), observerContent);
    }

    private String createConversation(Set<String> participants, Set<String> overseers) {
        String conversationId = UUID.randomUUID().toString();

        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setParticipants(List.copyOf(participants));
        conversation.setOverseers(List.copyOf(overseers));
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        conversation.setStatus("active");

        conversationRepository.persist(conversation)
                .await().atMost(Duration.ofSeconds(10));

        return conversationId;
    }

    private void assertPersistedInBothStores(String conversationId, String userId, String content) {
        Instant start = Instant.now();
        Instant from = start.minusSeconds(120);
        Instant to = start.plusSeconds(120);

        boolean foundInMongo = false;
        boolean foundInPostgres = false;
        Instant timeoutAt = Instant.now().plusSeconds(20);

        while (Instant.now().isBefore(timeoutAt)) {
            List<EventStore.ChatEventRecord> mongoEvents = mongoEventStore.getEvents(conversationId, from, to)
                    .await().atMost(Duration.ofSeconds(5));
            List<EventStore.ChatEventRecord> postgresEvents = postgresEventStore.getEvents(conversationId, from, to)
                    .await().atMost(Duration.ofSeconds(5));

            foundInMongo = mongoEvents.stream()
                    .anyMatch(event -> content.equals(event.payload()) && userId.equals(event.userId()));
            foundInPostgres = postgresEvents.stream()
                    .anyMatch(event -> content.equals(event.payload()) && userId.equals(event.userId()));

            if (foundInMongo && foundInPostgres) {
                break;
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for persistence assertions", interruptedException);
            }
        }

        assertTrue(foundInMongo, "Expected event in MongoDB for user " + userId);
        assertTrue(foundInPostgres, "Expected event in PostgreSQL for user " + userId);
    }
}
