package com.komeowmeow.qqchatbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.komeowmeow.qqchatbot.application.ConversationService;
import com.komeowmeow.qqchatbot.application.ReplyErrorMessages;
import com.komeowmeow.qqchatbot.application.UserQuestion;
import com.komeowmeow.qqchatbot.config.BotConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class QqEventListener {
    private static final Logger log = LoggerFactory.getLogger(QqEventListener.class);

    private final QqClient qqClient;
    private final BotConfig botConfig;
    private final ConversationService conversationService;
    private final GroupQuestionAssembler questionAssembler;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public QqEventListener(
            QqClient qqClient,
            BotConfig botConfig,
            ConversationService conversationService,
            GroupQuestionAssembler questionAssembler) {
        this.qqClient = qqClient;
        this.botConfig = botConfig;
        this.conversationService = conversationService;
        this.questionAssembler = questionAssembler;
    }

    @EventListener
    public void onOneBotEvent(OneBotEvent event) {
        JsonNode payload = event.payload();
        if (!"message".equals(payload.path("post_type").asText())
                || !"group".equals(payload.path("message_type").asText())) {
            return;
        }

        long senderId = payload.path("user_id").asLong();
        if (senderId == botConfig.getSelfId()) {
            return;
        }
        long groupId = payload.path("group_id").asLong();

        questionAssembler.assembleAsync(payload)
                .thenCompose(result -> {
                    if (result.imageIssue() == GroupQuestionAssembler.AssembleResult.ImageIssue.TOO_LARGE) {
                        return qqClient.sendGroupMessage(groupId, "图太大了喵，换小一点的或压缩后再发～");
                    }
                    if (result.imageIssue() == GroupQuestionAssembler.AssembleResult.ImageIssue.UNAVAILABLE) {
                        return qqClient.sendGroupMessage(groupId, "这张图我这边打不开喵，重新发一张试试？");
                    }
                    if (result.question() == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    UserQuestion question = result.question();
                    if (question.hasImages()) {
                        log.info("收到群 {} 用户 {} 的消息（含 {} 张图）：{}", groupId, senderId,
                                question.images().size(), question.effectiveText());
                    } else {
                        log.info("收到群 {} 用户 {} 的消息：{}", groupId, senderId, question.effectiveText());
                    }
                    return CompletableFuture
                            .supplyAsync(() -> conversationService.reply(groupId, senderId, question), executor)
                            .thenCompose(answer -> qqClient.sendGroupMessage(groupId, answer));
                })
                .handle((ignored, error) -> {
                    if (error == null) {
                        return null;
                    }
                    log.error("群 {} 用户 {} 消息处理失败", groupId, senderId, error);
                    String hint = ReplyErrorMessages.toUserMessage(error);
                    return qqClient.sendGroupMessage(groupId, hint)
                            .exceptionally(sendError -> {
                                log.error("群 {} 发送失败提示也失败了", groupId, sendError);
                                return null;
                            });
                })
                .thenCompose(stage -> stage == null
                        ? CompletableFuture.completedFuture(null)
                        : stage);
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
