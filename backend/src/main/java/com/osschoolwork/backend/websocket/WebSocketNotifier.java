package com.osschoolwork.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketNotifier {

    private final WebSocketSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public WebSocketNotifier(WebSocketSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public void notifyNewMail(Collection<Long> receiverIds, Long mailId, Long senderId, String subject) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_MAIL");
        payload.put("mailId", mailId);
        payload.put("senderId", senderId);
        payload.put("subject", subject == null ? "" : subject);

        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        for (Long receiverId : receiverIds) {
            for (WebSocketSession session : registry.getSessions(receiverId)) {
                if (!session.isOpen()) {
                    continue;
                }
                try {
                    session.sendMessage(textMessage);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
