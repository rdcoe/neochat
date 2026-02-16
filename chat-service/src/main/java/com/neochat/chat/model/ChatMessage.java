package com.neochat.chat.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chat message payload
 */
public class ChatMessage {
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("identity_token")
    private String identityToken;

    private String content;

    @JsonProperty("message_type")
    private String messageType;

    public ChatMessage() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getIdentityToken() {
        return identityToken;
    }

    public void setIdentityToken(String identityToken) {
        this.identityToken = identityToken;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
