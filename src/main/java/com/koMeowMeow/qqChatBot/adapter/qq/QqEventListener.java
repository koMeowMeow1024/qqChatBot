package com.komeowmeow.qqchatbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.komeowmeow.qqchatbot.application.ConversationService;
import com.komeowmeow.qqchatbot.config.BotConfig;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;

@Component
public class QqEventListener {
    private static final Logger log= LoggerFactory.getLogger(QqEventListener.class);
    private final QqClient qqClient;
    private final BotConfig botConfig;
    private final ConversationService conversationService;

    public QqEventListener(QqClient qqClient, BotConfig botConfig, ConversationService conversationService) {
        this.qqClient = qqClient;
        this.botConfig = botConfig;
        this.conversationService = conversationService;
    }
    @EventListener
    public void onOneBotEvent(OneBotEvent event){
        JsonNode payload=event.payload();
        if(!"message".equals(payload.path("post_type").asText())||!"group".equals(payload.path("message_type").asText()));

        long senderId=payload.path("user_id").asLong();
        if(senderId==botConfig.getSelfId())return;
//        log.info("收到{}群{}用户的消息{}",groupId,senderId,question);
    }
}
