package com.komeowmeow.qqchatbot.application;

import com.komeowmeow.qqchatbot.infrastructure.persistence.ChatMessageRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final MessageHandler messageHandler;
    private final ChatMessageRepository chatMessageRepository;

    public ConversationService(MessageHandler messageHandler,ChatMessageRepository chatMessageRepository){
        this.messageHandler=messageHandler;
        this.chatMessageRepository=chatMessageRepository;
    }

    public String reply(long groupId,long userId,String question) {
        chatMessageRepository.save(groupId,userId,"user",question);
        String answer=messageHandler.handle(question);
        chatMessageRepository.save(groupId,userId,"assistant",question);
        return answer;
    }
}
