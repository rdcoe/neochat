package com.neochat.chat.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neochat.chat.model.ChatMessage;
import com.neochat.chat.service.ChatEventService;
import com.neochat.common.security.model.IdentityTokenClaims;
import com.neochat.common.security.service.TokenValidator;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateless WebSocket endpoint for chat
 * Authentication via JWT in connection header
 * Authorization per message via identity_token
 */
@ServerEndpoint("/api/chat/{conversationId}")
@ApplicationScoped
public class ChatWebSocketEndpoint {
    private static final Logger LOG = Logger.getLogger(ChatWebSocketEndpoint.class);

    @Inject
    TokenValidator tokenValidator;

    @Inject
    ChatEventService chatEventService;

    @Inject
    ObjectMapper objectMapper;

    // Track active sessions per conversation (for broadcasting)
    private final Map<String, Map<String, Session>> conversationSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("conversationId") String conversationId) {
        // Validate JWT from Authorization header
        Map<String, String> params = session.getRequestParameterMap().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().isEmpty() ? "" : e.getValue().getFirst()
                ));

        String authHeader = params.get("authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.warn("WebSocket connection attempt without valid authorization");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Missing authorization"));
            } catch (IOException e) {
                LOG.error("Failed to close session", e);
            }
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer "
        try {
            tokenValidator.validateToken(token);
            
            // Store session
            conversationSessions.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
            
            LOG.infof("WebSocket connection opened for conversation %s, session %s", 
                     conversationId, session.getId());
                     
        } catch (ParseException e) {
            LOG.error("Invalid JWT token", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            } catch (IOException ex) {
                LOG.error("Failed to close session", ex);
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("conversationId") String conversationId) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

            // Validate identity token from message
            String identityToken = chatMessage.getIdentityToken();
            if (identityToken == null) {
                sendError(session, "Missing identity_token in message");
                return;
            }

            IdentityTokenClaims claims = tokenValidator.extractIdentityClaims(identityToken);

            // Verify user can access this conversation
            chatEventService.validateAccess(claims.getSubject(), conversationId)
                    .subscribe().with(
                        canAccess -> {
                            if (!canAccess) {
                                sendError(session, "User does not have access to this conversation");
                                return;
                            }

                            // Post message
                            chatEventService.postMessage(
                                conversationId,
                                claims.getSubject(),
                                chatMessage.getContent(),
                                chatMessage.getMessageType()
                            ).subscribe().with(
                                success -> {
                                    LOG.debugf("Message posted successfully to conversation %s", conversationId);
                                    // Broadcasting handled by Kafka consumer
                                },
                                error -> {
                                    LOG.error("Failed to post message", error);
                                    sendError(session, "Failed to post message: " + error.getMessage());
                                }
                            );
                        },
                        error -> {
                            LOG.error("Failed to validate access", error);
                            sendError(session, "Failed to validate access");
                        }
                    );

        } catch (Exception e) {
            LOG.error("Failed to process message", e);
            sendError(session, "Invalid message format");
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("conversationId") String conversationId) {
        Map<String, Session> sessions = conversationSessions.get(conversationId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                conversationSessions.remove(conversationId);
            }
        }
        LOG.infof("WebSocket connection closed for conversation %s, session %s", 
                 conversationId, session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("conversationId") String conversationId) {
        LOG.errorf(throwable, "WebSocket error for conversation %s, session %s", 
                  conversationId, session.getId());
    }

    /**
     * Broadcast message to all sessions in a conversation
     */
    public void broadcast(String conversationId, String message) {
        Map<String, Session> sessions = conversationSessions.get(conversationId);
        if (sessions != null) {
            sessions.values().forEach(session -> {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                }
            });
        }
    }

    private void sendError(Session session, String error) {
        try {
            session.getBasicRemote().sendText(
                objectMapper.writeValueAsString(Map.of("error", error))
            );
        } catch (Exception e) {
            LOG.error("Failed to send error message", e);
        }
    }
}
