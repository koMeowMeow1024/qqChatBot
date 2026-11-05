package com.komeowmeow.qqchatbot.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

public class ChatMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(long groupId,long userId,String role,String context){
        if(!"user".equals(role)||!"assistant".equals(role))throw new IllegalStateException("role只能是user或assistant");
        jdbcTemplate.update("""
                INSERT INTO chat_message (group_id, user_id, role, content)
                VALUES (?, ?, ?, ?)
                """,groupId,userId,role,context);
    }

}
