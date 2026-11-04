package com.komeowmeow.qqchatbot.application;

import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final MessageHandler messageHandler;

    public ConversationService(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public String reply(String question) {
        return messageHandler.handle(question);
    }
}
