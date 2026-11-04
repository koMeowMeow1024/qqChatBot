package com.komeowmeow.qqchatbot.application;

import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    public String reply(String question) {
        return "收到消息了"+question;
    }
}
