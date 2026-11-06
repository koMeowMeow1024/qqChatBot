package com.komeowmeow.qqchatbot.application;
import com.komeowmeow.qqchatbot.infrastructure.persistence.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private final MessageHandler messageHandler;
    private final ChatMessageRepository chatMessageRepository;

    public ConversationService(MessageHandler messageHandler,ChatMessageRepository chatMessageRepository){
        this.messageHandler=messageHandler;
        this.chatMessageRepository=chatMessageRepository;
    }

    public String reply(long groupId,long userId,String question) {
        List<ChatMessageRepository.ChatMessage>history=chatMessageRepository.findRecentByGroupId(groupId,10);
        String context=history.stream()
                        .map(message->message.role()+":"+message.context())
                .collect(Collectors.joining("\n"));
        chatMessageRepository.save(groupId,userId,"user",question);
        String answer=messageHandler.handle(question,context);
        chatMessageRepository.save(groupId,userId,"assistant",question);
        return answer;
    }
}
