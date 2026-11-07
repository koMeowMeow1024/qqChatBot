CREATE TABLE IF NOT EXISTS chat_message (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        group_id INTEGER NOT NULL,
        user_id INTEGER NOT NULL,
        role TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 查询按 (group_id, id) 取最近记录，旧索引按时间建，重建为匹配查询的索引
DROP INDEX IF EXISTS idx_chat_message_group_time;
CREATE INDEX IF NOT EXISTS idx_chat_message_group_id
    ON chat_message(group_id, id);
