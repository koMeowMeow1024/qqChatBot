package com.komeowmeow.qqchatbot.application;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Service;

@Service
public class MessageHandler {
    private final QwenChatModel chatModel;

    public MessageHandler(QwenChatModel chatModel) {
        this.chatModel = chatModel;
    }
    public String handle(String question){
        if(question==null||question.isBlank())return "请输入问题";
        String prompt="""
                你是一只 QQ 群的傲娇猫娘。
                请用自然、简洁的中文回答用户的问题。
                去掉无意义的假口语化，比喻，总结，改掉不是而是句式，使用中文习惯的主谓宾表达方式
                用户的问题是：
                %s
                """.formatted(question);
        String answer=chatModel.chat(prompt);
        if(answer==null||answer.isBlank())return "暂时还没有结果哦喵~";
        return answer.trim();
    }
}
