package com.komeowmeow.qqchatbot.application;

import com.komeowmeow.qqchatbot.config.BotLimitsProperties;
import com.komeowmeow.qqchatbot.infrastructure.persistence.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {
    private static final int LOCK_STRIPES = 64;

    private final MessageHandler messageHandler;
    private final ChatMessageRepository chatMessageRepository;
    private final BotLimitsProperties limits;
    private final Object[] groupLocks = new Object[LOCK_STRIPES];

    public ConversationService(
            MessageHandler messageHandler,
            ChatMessageRepository chatMessageRepository,
            BotLimitsProperties limits) {
        this.messageHandler = messageHandler;
        this.chatMessageRepository = chatMessageRepository;
        this.limits = limits;
        for (int i = 0; i < groupLocks.length; i++) {
            groupLocks[i] = new Object();
        }
    }

    public String reply(long groupId, long userId, UserQuestion question) {
        // 同一个群的消息串行处理，避免上下文和写入顺序互相穿插
        Object lock = groupLocks[Math.floorMod(groupId, LOCK_STRIPES)];
        synchronized (lock) {
            int messageLimit = Math.min(Math.max(limits.getContextMaxMessages(), 1), 50);
            List<ChatMessageRepository.ChatMessage> history =
                    chatMessageRepository.findRecentByGroupId(groupId, messageLimit);
            String context = ChatContextFormatter.format(history, limits.getContextMaxChars());
            String memberDirectory = chatMessageRepository.findRecentGroupMembers(groupId, 200).stream()
                    .map(member -> member.userName() + "（QQ号：" + member.userId() + "）")
                    .collect(java.util.stream.Collectors.joining("\n"));

            String answer = messageHandler.handle(question, context, memberDirectory, userId);

            // AI 调用成功后再落库，避免留下只有提问没有回答的记录
            chatMessageRepository.save(groupId, userId, question.senderName(), "user", question.toStoredContent());
            chatMessageRepository.save(groupId, userId, question.senderName(), "assistant", answer);
            return answer;
        }
    }
}
