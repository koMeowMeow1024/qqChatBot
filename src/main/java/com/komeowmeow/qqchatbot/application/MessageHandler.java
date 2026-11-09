package com.komeowmeow.qqchatbot.application;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class MessageHandler {
    private final QwenChatModel chatModel;
    private final String systemPrompt;
    private final long replyTimeoutSeconds;
    private final ExecutorService aiExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public MessageHandler(
            QwenChatModel chatModel,
            @Value("${ai.dashscope.reply-timeout-seconds:90}") long replyTimeoutSeconds) {
        this.chatModel = chatModel;
        this.replyTimeoutSeconds = replyTimeoutSeconds;
        this.systemPrompt = loadSystemPrompt();
    }

    @PreDestroy
    public void close() {
        aiExecutor.shutdownNow();
    }

    private String loadSystemPrompt() {
        try (var input = new ClassPathResource("prompts/system-prompt.txt").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new IllegalStateException("无法获取 system-prompt.txt 的内容", e);
        }
    }

    public String handle(UserQuestion question, String context, String memberDirectory, long userId) {
        if (question.isBlank()) {
            return "请输入问题";
        }

        String userText = """
                以下是该群最近的聊天记录：
                %s

                以下是数据库中已记录的群友名册（昵称和 QQ 号）：
                %s
                用户询问群友昵称或身份时，优先根据这份名册回答；如果名册里没有对应记录，明确说明暂时没有记录，不要猜测。

                当前提问者：%s（QQ号：%s）
                请结合聊天记录回答当前问题：
                %s
                """.formatted(context, memberDirectory.isBlank() ? "暂无已记录的群友" : memberDirectory,
                question.senderName().isBlank() ? "未知群友" : question.senderName(),
                userId, question.effectiveText());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        if (question.hasImages()) {
            List<Content> contents = new ArrayList<>();
            contents.add(TextContent.from(userText));
            for (ImageAttachment image : question.images()) {
                contents.add(ImageContent.from(image.base64Data(), image.mimeType()));
            }
            messages.add(UserMessage.from(contents));
        } else {
            messages.add(UserMessage.from(userText));
        }

        ChatResponse response;
        try {
            response = CompletableFuture
                    .supplyAsync(() -> chatModel.chat(messages), aiExecutor)
                    .orTimeout(replyTimeoutSeconds, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            if (cause instanceof TimeoutException) {
                throw new AiReplyException(ReplyErrorMessages.toUserMessage(cause), cause);
            }
            throw new AiReplyException("模型调用失败了喵，稍后再试～", cause);
        }
        String answer = response.aiMessage().text();
        if (answer == null || answer.isBlank()) {
            return "暂时还没有结果哦喵~";
        }
        return answer.trim();
    }
}
