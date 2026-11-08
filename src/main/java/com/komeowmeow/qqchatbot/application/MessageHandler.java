package com.komeowmeow.qqchatbot.application;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageHandler {
    private final QwenChatModel chatModel;
    private final String systemPrompt;

    public MessageHandler(QwenChatModel chatModel) {
        this.chatModel = chatModel;
        this.systemPrompt = loadSystemPrompt();
    }

    private String loadSystemPrompt() {
        try (var input = new ClassPathResource("prompts/system-prompt.txt").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new IllegalStateException("无法获取 system-prompt.txt 的内容", e);
        }
    }

    public String handle(UserQuestion question, String context) {
        if (question.isBlank()) {
            return "请输入问题";
        }

        String userText = """
                以下是该群最近的聊天记录：
                %s

                请结合聊天记录回答当前问题：
                %s
                """.formatted(context, question.effectiveText());

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

        ChatResponse response = chatModel.chat(messages);
        String answer = response.aiMessage().text();
        if (answer == null || answer.isBlank()) {
            return "暂时还没有结果哦喵~";
        }
        return answer.trim();
    }
}
