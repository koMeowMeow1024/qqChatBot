package com.komeowmeow.qqchatbot.adapter.qq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class OneBotListener implements WebSocket.Listener {
    private static final Logger log = LoggerFactory.getLogger(OneBotListener.class);

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final StringBuilder textBuffer = new StringBuilder();

    public OneBotListener(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        log.info("OneBot WebSocket 已打开");
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            String message = textBuffer.toString();
            textBuffer.setLength(0);
            try {
                JsonNode event = objectMapper.readTree(message);
                if (event != null) {
                    eventPublisher.publishEvent(event);
                }
            } catch (JsonProcessingException e) {
                log.error("无法解析 OneBot WebSocket 消息: {}", message, e);
            }
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        log.warn("忽略 OneBot WebSocket 二进制消息");
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.info("OneBot WebSocket 已关闭，状态码: {}, 原因: {}", statusCode, reason);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("OneBot WebSocket 发生错误", error);
    }


}
