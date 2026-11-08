package com.komeowmeow.qqchatbot.application;

import com.komeowmeow.qqchatbot.infrastructure.persistence.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private static final int LOCK_STRIPES = 64;

    private final MessageHandler messageHandler;
    private final ChatMessageRepository chatMessageRepository;
    private final Object[] groupLocks = new Object[LOCK_STRIPES];

    public ConversationService(MessageHandler messageHandler, ChatMessageRepository chatMessageRepository) {
        this.messageHandler = messageHandler;
        this.chatMessageRepository = chatMessageRepository;
        for (int i = 0; i < groupLocks.length; i++) {
            groupLocks[i] = new Object();
        }
    }

    public String reply(long groupId, long userId, UserQuestion question) {
        // 同一个群的消息串行处理，避免上下文和写入顺序互相穿插
        Object lock = groupLocks[Math.floorMod(groupId, LOCK_STRIPES)];
        synchronized (lock) {
            List<ChatMessageRepository.ChatMessage> history =
                    chatMessageRepository.findRecentByGroupId(groupId, 10);
            String context = history.stream()
                    .map(message -> ("user".equals(message.role()) ? "用户" : "你")
                            + "：" + message.content())
                    .collect(Collectors.joining("\n"));

            String answer = messageHandler.handle(question, context);

            // AI 调用成功后再落库，避免留下只有提问没有回答的记录
            chatMessageRepository.save(groupId, userId, "user", question.toStoredContent());
            chatMessageRepository.save(groupId, userId, "assistant", answer);
            return answer;
        }
    }
}
