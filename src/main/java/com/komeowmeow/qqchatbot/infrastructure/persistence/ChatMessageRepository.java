package com.komeowmeow.qqchatbot.infrastructure.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@DependsOnDatabaseInitialization
public class ChatMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureUserNameColumn() {
        Set<String> columns = Set.copyOf(jdbcTemplate.query(
                "PRAGMA table_info(chat_message)",
                (resultSet, rowNum) -> resultSet.getString("name")));
        if (!columns.contains("user_name")) {
            jdbcTemplate.execute("ALTER TABLE chat_message ADD COLUMN user_name TEXT NOT NULL DEFAULT ''");
        }
    }

    public void save(long groupId, long userId, String userName, String role, String content) {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalStateException("role 只能是 user 或 assistant");
        }
        jdbcTemplate.update("""
                INSERT INTO chat_message (group_id, user_id, user_name, role, content)
                VALUES (?, ?, ?, ?, ?)
                """, groupId, userId, userName == null ? "" : userName, role, content);
    }

    public List<ChatMessage> findRecentByGroupId(long groupId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        return jdbcTemplate.query("""
                SELECT group_id, user_id, user_name, role, content, created_at
                FROM (
                    SELECT group_id, user_id, user_name, role, content, created_at, id
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
                        resultSet.getString("user_name"),
                        resultSet.getString("role"),
                        resultSet.getString("content"),
                        resultSet.getString("created_at")
                ),
                groupId,
                safeLimit
        );
    }

    public List<GroupMember> findRecentGroupMembers(long groupId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jdbcTemplate.query("""
                SELECT message.user_id, message.user_name
                FROM chat_message AS message
                JOIN (
                    SELECT user_id, MAX(id) AS latest_id
                    FROM chat_message
                    WHERE group_id = ? AND role = 'user' AND user_name <> ''
                    GROUP BY user_id
                ) AS latest ON message.id = latest.latest_id
                ORDER BY latest.latest_id DESC
                LIMIT ?
                """,
                (resultSet, rowNum) -> new GroupMember(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name")),
                groupId,
                safeLimit);
    }

    public record ChatMessage(
            long groupId, long userId, String userName, String role, String content, String createdAt) {
    }

    public record GroupMember(long userId, String userName) {
    }
}
