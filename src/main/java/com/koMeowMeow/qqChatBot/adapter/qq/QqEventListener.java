package com.komeowmeow.qqchatbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.komeowmeow.qqchatbot.application.ConversationService;
import com.komeowmeow.qqchatbot.config.BotConfig;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

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
        if(!"message".equals(payload.path("post_type").asText())||!"group".equals(payload.path("message_type").asText()))return;

        long senderId=payload.path("user_id").asLong();
        if(senderId==botConfig.getSelfId())return;
        long groupId=payload.path("group_id").asLong();
        String question=extractQuestion(payload);
        log.info("收到{}群{}用户的消息{}",groupId,senderId,question);

        CompletableFuture
                .supplyAsync(()-> conversationService.reply(question))
                .thenCompose(answer->qqClient.sendGroupMessage(groupId,answer))
                .exceptionally(error->{log.error("qq消息处理失败",error);return null;});
    }
    private String extractQuestion(JsonNode payload){
        JsonNode message=payload.path("message");
        if(message.isArray()){
            boolean metionedBot=false;
            StringBuilder text=new StringBuilder();

            for(JsonNode segment:message){
                String type=segment.path("type").asText();
                if("at".equals(type)&&String.valueOf(botConfig.getSelfId()).equals(segment.path("data").path("qq").asText())){
                    metionedBot=true;
                    continue;
                }
                if("text".equals(type)){
                    text.append(segment.path("data").path("text").asText());
                }
            }
            return metionedBot?text.toString().trim():null;
        }
        String rawMessage=payload.path("raw_message").asText();
        String atCode="[CQ:at,qq="+botConfig.getSelfId()+"]";
        if(!rawMessage.contains(atCode))return null;

        return rawMessage.replace(atCode,"").trim();
    }
}
