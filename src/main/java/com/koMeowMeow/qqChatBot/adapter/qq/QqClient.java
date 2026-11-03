package com.komeowmeow.qqchatbot.adapter.qq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.komeowmeow.qqchatbot.config.BotConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public void connect() {
        if (!botConfig.isEnabled()) {
            log.info("QQ Bot 暂未启用");
            return;
        }
        httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer" + botConfig.getAccessToken())
                .buildAsync(botConfig.getOneBotWebSocketUrl(), new OneBotListener(objectMapper, eventPublisher))
                .whenComplete((socket, error) -> {
                    if (error != null) {
                        log.error("无法连接 NapCat OneBot WebSocket", error);
                        return;
                    }

                    this.webSocket = socket;
                    log.info("已连接 NapCat软件：{}", botConfig.getOneBotWebSocketUrl());
                });
    }
        public CompletableFuture<Void> sendGroupMessage(long groupId,String text){
            if(text==null||text.isBlank()){
                return CompletableFuture.failedFuture(
                    new IllegalStateException("回复内容不能为空"));
            }
            WebSocket socket=webSocket;
            if(socket==null)return CompletableFuture.failedFuture(
              new IllegalStateException("未连接到Napcat")
            );

            ObjectNode params=objectMapper.createObjectNode();
            params.put("group_id",groupId);
            params.put("message",text);
            params.put("auto-escape",true);

            ObjectNode request=objectMapper.createObjectNode();
            request.put("action","send_group_msp");
            request.put("params",params);
            request.put("echo", UUID.randomUUID().toString());

            try{
                String payload=objectMapper.writeValueAsString(request);
                return socket.sendText(payload,true).thenApply(ignored->null);
            }catch(JsonProcessingException expection){
                return CompletableFuture.failedFuture(expection);
            }
        }
        @PreDestroy
        public void close(){
            WebSocket socket=webSocket;
            if(socket!=null)socket.sendClose(WebSocket.NORMAL_CLOSURE,"application is stop");
        }
        private void handleInComingText(String text){
            try{
                JsonNode payload=objectMapper.readTree(text);
                if(payload.has("post_type")){
                    eventPublisher.publishEvent(new OneBotEvent(payload));
                    return;
                }
                if("failed".equals(payload.path("status").asText())){
                    log.warn("OneBotEvent 请求失败:{}",payload);
                }
            }catch(JsonProcessingException exception){
                log.warn("OneBotEvent 解析失败:{}",text,exception);
            }
        }
        private class OneBotListener implements WebSocket.Listener{
            @Override
            public void onOpen(WebSocket socket){
                socket.request(1);
            }
            @Override
            public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last){
                String completeText=null;
                synchronized(textBuffer) {
                    textBuffer.append(data);
                    if(last){
                        completeText=textBuffer.toString();
                        textBuffer.setLength(0);
                    }
                }
                if(completeText!=null)handleInComingText(completeText);
                socket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            @Override
            public void onError(WebSocket socket,Throwable error){log.error("Navicat websocket发生错误",error);}

            @Override
            public CompletionStage<?> onClose(WebSocket socket,int StatusCode,String reason){
                webSocket=null;
                log.warn("navicat webSocket已断开：{}，{}",StatusCode,reason);
                return CompletableFuture.completedFuture(null);
            }
        }
}
