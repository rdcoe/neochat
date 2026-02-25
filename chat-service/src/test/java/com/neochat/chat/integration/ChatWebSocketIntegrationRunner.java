package com.neochat.chat.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.common.security.service.MockPersonaTokenValidator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Helper runner for WebSocket integration scenarios.
 */
public class ChatWebSocketIntegrationRunner {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public void sendMessage(URI httpBaseUri, String conversationId, Persona persona, String content) {
        String token = MockPersonaTokenValidator.buildToken(
                persona.subject(),
                persona.email(),
                persona.roles(),
                persona.groups());

        URI wsUri = URI.create(toWebSocketBase(httpBaseUri) + "/api/chat/" + conversationId);
        BlockingQueue<String> inboundMessages = new LinkedBlockingQueue<>();
        CompletableFuture<Void> connected = new CompletableFuture<>();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                connected.complete(null);
                WebSocket.Listener.super.onOpen(webSocket);
            }

            @Override
            public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                inboundMessages.offer(data.toString());
                return CompletableFuture.completedFuture(null);
            }
        };

        WebSocket webSocket = client.newWebSocketBuilder()
                .header("Authorization", "Bearer " + token)
                .buildAsync(wsUri, listener)
                .join();

        connected.join();

        String payload = buildPayload(conversationId, token, content);
        webSocket.sendText(payload, true).join();

        String response;
        try {
            response = inboundMessages.poll(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for WebSocket response", interruptedException);
        }

        if (response != null && response.contains("\"error\"")) {
            throw new AssertionError("WebSocket returned error payload: " + response);
        }

        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    private String buildPayload(String conversationId, String identityToken, String content) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "conversation_id", conversationId,
                    "identity_token", identityToken,
                    "content", content,
                    "message_type", "message"));
        } catch (JsonProcessingException jsonProcessingException) {
            throw new RuntimeException("Failed to create test message payload", jsonProcessingException);
        }
    }

    private String toWebSocketBase(URI httpBaseUri) {
        String scheme = "https".equalsIgnoreCase(httpBaseUri.getScheme()) ? "wss" : "ws";
        int port = httpBaseUri.getPort();
        String authority = port > 0
                ? httpBaseUri.getHost() + ":" + port
                : httpBaseUri.getHost();
        return scheme + "://" + authority;
    }

    public record Persona(String subject, String email, Set<String> roles, Set<String> groups) {
    }
}
