package com.komeowmeow.qqchatbot.application;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Service
public class MessageHandler {
    private String loadSystemPrompt() {
        try(var input=new ClassPathResource("prompts/system-prompt.txt").getInputStream()){
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }catch(Exception e){
            throw new IllegalStateException("无法获取system-prompt.txt的内容",e);
        }
    }

    private final QwenChatModel chatModel;

    public MessageHandler(QwenChatModel chatModel) {
        this.chatModel = chatModel;
    }
    public String handle(String question){
        if(question==null||question.isBlank())return "请输入问题";
        String prompt=loadSystemPrompt()+"\n\n用户的问题是：\n"+question;
        String answer=chatModel.chat(prompt);
        if(answer==null||answer.isBlank())return "暂时还没有结果哦喵~";
        return answer.trim();
    }
}
