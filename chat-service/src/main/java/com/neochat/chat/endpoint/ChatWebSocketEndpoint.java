package com.neochat.chat.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.chat.model.ChatMessage;
import com.neochat.chat.service.ChatEventService;
import com.neochat.common.security.model.IdentityTokenClaims;
import com.neochat.common.security.service.TokenValidator;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Stateless WebSocket endpoint for chat
 * Authentication via JWT in connection header
 * Authorization per message via identity_token
 */
@WebSocket(path = "/api/chat/{conversationId}")
public class ChatWebSocketEndpoint {
    private static final Logger LOG = Logger.getLogger(ChatWebSocketEndpoint.class);
    private static final String CONVERSATION_ID = "conversationId";

    @Inject
    TokenValidator tokenValidator;

    @Inject
    ChatEventService chatEventService;

    @Inject
    ObjectMapper objectMapper;

    // Track active connections per conversation (for broadcasting)
    private final Map<String, Map<String, WebSocketConnection>> conversationConnections = new ConcurrentHashMap<>();

    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection) {
        String conversationId = connection.pathParam(CONVERSATION_ID);

        // Validate JWT from query param (WebSocket clients typically pass token as
        // query param)
        String token = connection.handshakeRequest().query() != null
                ? extractTokenFromQuery(connection.handshakeRequest().query())
                : null;

        if (token == null) {
            String authHeader = connection.handshakeRequest().header("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            LOG.warn("WebSocket connection attempt without valid authorization");
            return connection.close();
        }

        try {
            tokenValidator.validateToken(token);

            // Store connection
            conversationConnections.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                    .put(connection.id(), connection);

            LOG.infof("WebSocket connection opened for conversation %s, connection %s",
                    conversationId, connection.id());
            return Uni.createFrom().voidItem();

        } catch (ParseException e) {
            LOG.error("Invalid JWT token", e);
            return connection.close();
        }
    }

    @OnTextMessage
    public Uni<Void> onMessage(String message, WebSocketConnection connection) {
        String conversationId = connection.pathParam(CONVERSATION_ID);
        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

            // Validate identity token from message
            String identityToken = chatMessage.getIdentityToken();
            if (identityToken == null) {
                return sendError(connection, "Missing identity_token in message");
            }

            IdentityTokenClaims claims = tokenValidator.extractIdentityClaims(identityToken);

            // Verify user can access this conversation
            return chatEventService.validateAccess(claims.getSubject(), conversationId)
                    .flatMap(canAccess -> {
                        if (!canAccess) {
                            return sendError(connection, "User does not have access to this conversation");
                        }

                        // Post message
                        return chatEventService.postMessage(
                                conversationId,
                                claims.getSubject(),
                                chatMessage.getContent(),
                                chatMessage.getMessageType())
                                .invoke(() -> LOG.debugf("Message posted successfully to conversation %s",
                                        conversationId))
                                .onFailure().recoverWithUni(error -> {
                                    LOG.error("Failed to post message", error);
                                    return sendError(connection, "Failed to post message: " + error.getMessage());
                                });
                    })
                    .onFailure().recoverWithUni(error -> {
                        LOG.error("Failed to validate access", error);
                        return sendError(connection, "Failed to validate access");
                    });

        } catch (Exception e) {
            LOG.error("Failed to process message", e);
            return sendError(connection, "Invalid message format");
        }
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        String conversationId = connection.pathParam(CONVERSATION_ID);
        Map<String, WebSocketConnection> connections = conversationConnections.get(conversationId);
        if (connections != null) {
            connections.remove(connection.id());
            if (connections.isEmpty()) {
                conversationConnections.remove(conversationId);
            }
        }
        LOG.infof("WebSocket connection closed for conversation %s, connection %s",
                conversationId, connection.id());
    }

    @OnError
    public void onError(WebSocketConnection connection, Throwable throwable) {
        String conversationId = connection.pathParam(CONVERSATION_ID);
        LOG.errorf(throwable, "WebSocket error for conversation %s, connection %s",
                conversationId, connection.id());
    }

    /**
     * Broadcast message to all connections in a conversation
     */
    public Uni<Void> broadcast(String conversationId, String message) {
        Map<String, WebSocketConnection> connections = conversationConnections.get(conversationId);
        if (connections != null) {
            return Uni.join().all(
                    connections.values().stream()
                            .filter(WebSocketConnection::isOpen)
                            .map(conn -> conn.sendText(message))
                            .toList())
                    .andFailFast().replaceWithVoid();
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> sendError(WebSocketConnection connection, String error) {
        try {
            String errorJson = objectMapper.writeValueAsString(Map.of("error", error));
            return connection.sendText(errorJson);
        } catch (Exception e) {
            LOG.error("Failed to send error message", e);
            return Uni.createFrom().voidItem();
        }
    }

    private String extractTokenFromQuery(String query) {
        if (query == null)
            return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
