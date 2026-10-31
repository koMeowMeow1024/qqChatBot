package com.komeowmeow.qqchatbot;

import com.komeowmeow.qqchatbot.config.BotConfig;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BotConfig.class)
public class QqChatBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(QqChatBotApplication.class, args);
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("api is wrong");
        QwenChatModel model = QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen3.7-flash")
                .build();
        String answer = model.chat("你好啊");
        System.out.println(answer);
    }
}
