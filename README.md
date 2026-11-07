# qqChatBot

一个基于 Spring Boot + LangChain4j + NapCat(OneBot v11) 的 QQ 群聊机器人。
收到 @机器人 的群消息后，会结合该群最近的聊天记录调用通义千问生成回复，并把对话存入 SQLite。

## 技术栈

- Java 21 / Spring Boot 3.5
- LangChain4j community dashscope（QwenChatModel）
- NapCat OneBot v11 WebSocket（JDK HttpClient WebSocket 客户端）
- SQLite + Spring JDBC

## 目录结构

```
qqChatBot/
├─ pom.xml
├─ .gitignore
├─ src/main/
│  ├─ java/com/komeowmeow/qqchatbot/
│  │  ├─ QqChatBotApplication.java
│  │  ├─ adapter/qq/            # OneBot 连接、事件接收、消息发送
│  │  │  ├─ QqClient.java
│  │  │  ├─ QqEventListener.java
│  │  │  └─ OneBotEvent.java
│  │  ├─ application/           # 业务编排：上下文、调用 AI、持久化
│  │  │  ├─ ConversationService.java
│  │  │  └─ MessageHandler.java
│  │  ├─ config/
│  │  │  ├─ AiConfig.java
│  │  │  └─ BotConfig.java
│  │  └─ infrastructure/
│  │     └─ persistence/
│  │        └─ ChatMessageRepository.java
│  └─ resources/
│     ├─ application.yml
│     ├─ schema.sql
│     └─ prompts/system-prompt.txt
└─ README.md
```

## 配置

通过环境变量注入，不要把密钥写进仓库：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key（必填） | 无 |
| `DASHSCOPE_MODEL` | 模型名称 | `qwen3.7-flash` |
| `ONEBOT_WS_URL` | NapCat 的 OneBot WebSocket 地址 | `ws://127.0.0.1:3001/` |
| `ONEBOT_ACCESS_TOKEN` | NapCat 配置的 access token，未配置时可不填 | 空 |

机器人 QQ 号在 `application.yml` 的 `bot.qq.self-id` 中配置，用于过滤自身消息和识别 @。

## 运行

```bash
# 1. 先启动 NapCat 并打开 OneBot v11 的 WebSocket 服务
# 2. 设置密钥
export DASHSCOPE_API_KEY=sk-xxx

# 3. 启动
mvn spring-boot:run
```

数据库文件 `qq-chat-bot.db` 会在首次启动时自动创建，已被 `.gitignore` 忽略。

## 行为说明

- 只处理群消息，且必须 @机器人 才会回复；私聊消息目前忽略。
- 每个群串行处理消息，避免同一群的上下文和写入顺序交错。
- 会取该群最近 10 条对话作为上下文；AI 调用成功后才写入数据库。
- WebSocket 断线后自动重连（退避 1s / 2s / 4s ... 最长 30s）。
