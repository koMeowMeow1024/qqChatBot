package com.komeowmeow.qqchatbot.adapter.qq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.komeowmeow.qqchatbot.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.WebSocket;

@Component
public class QqClient {
    private static final Logger log=LoggerFactory.getLogger(QqClient.class);
    private final BotConfig botConfig;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpClient httpClient=HttpClient.newHttpClient();
    private volatile WebSocket webSocket;

    public QqClient(BotConfig botConfig, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.botConfig = botConfig;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void connect(){
        if(!botConfig.isEnabled()){
            log.info("QQ Bot 暂未启用");
            return;
        }
        httpClient.newWebSocketBuilder()
                .header("Authorization","Bearer"+botConfig.getAccessToken())
                .buildAsync(botConfig.getOneBotWebSocketUrl(),new OneBotListener(objectMapper, eventPublisher))
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        log.error("无法连接 NapCat OneBot WebSocket", error);
                        return;
                    }

                    this.webSocket = socket;
                    log.info("已连接 NapCat软件：{}", botConfig.getOneBotWebSocketUrl());
                });
    }
}
