package com.komeowmeow.qqchatbot.application;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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

    public String handle(String question, String context) {
        if (question == null || question.isBlank()) {
            return "请输入问题";
        }
        String prompt = """
                %s

                以下是该群最近的聊天记录：
                %s

                请结合聊天记录回答当前问题：
                %s
                """.formatted(systemPrompt, context, question);

        String answer = chatModel.chat(prompt);
        if (answer == null || answer.isBlank()) {
            return "暂时还没有结果哦喵~";
        }
        return answer.trim();
    }
}
