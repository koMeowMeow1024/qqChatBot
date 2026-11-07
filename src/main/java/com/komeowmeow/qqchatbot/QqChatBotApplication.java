package com.komeowmeow.qqchatbot;

import com.komeowmeow.qqchatbot.config.BotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BotConfig.class)
public class QqChatBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(QqChatBotApplication.class, args);
    }
}
