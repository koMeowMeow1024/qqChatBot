package com.komeowmeow.qqchatbot.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(long groupId, long userId, String role, String content) {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalStateException("role 只能是 user 或 assistant");
        }
        jdbcTemplate.update("""
                INSERT INTO chat_message (group_id, user_id, role, content)
                VALUES (?, ?, ?, ?)
                """, groupId, userId, role, content);
    }

    public List<ChatMessage> findRecentByGroupId(long groupId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        return jdbcTemplate.query("""
                SELECT group_id, user_id, role, content, created_at
                FROM (
                    SELECT group_id, user_id, role, content, created_at, id
                    FROM chat_message
                    WHERE group_id = ?
                    ORDER BY id DESC
                    LIMIT ?
                )
                ORDER BY id ASC
                """,
                (resultSet, rowNum) -> new ChatMessage(
                        resultSet.getLong("group_id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("role"),
                        resultSet.getString("content"),
                        resultSet.getString("created_at")
                ),
                groupId,
                safeLimit
        );
    }

    public record ChatMessage(long groupId, long userId, String role, String content, String createdAt) {
    }
}
