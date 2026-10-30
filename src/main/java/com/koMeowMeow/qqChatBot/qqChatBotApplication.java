package com.komeowmeow.qqchatbot;
import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class qqChatBotApplication {
    public static void main(String[] args){
        String apiKey=System.getenv("DASHSCOPE_API_KEY");
        if(apiKey==null||apiKey.isBlank())throw new IllegalStateException("api is wrong");
        QwenChatModel model=QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3.7-flash")
                .build();
        String answer=model.chat("你好啊");
        System.out.println(answer);
    }
}
