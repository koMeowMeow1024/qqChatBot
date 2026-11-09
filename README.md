# qqChatBot

一个基于 Spring Boot + LangChain4j + NapCat（OneBot v11）的 QQ 群聊机器人。  
在群里 **@ 机器人** 后，会结合该群最近聊天记录调用通义千问生成回复；支持附带或回复引用的图片（多模态），对话写入 SQLite。

## 技术栈

- Java 21 / Spring Boot 3.5
- LangChain4j community dashscope（`QwenChatModel`）
- NapCat OneBot v11 WebSocket（JDK `HttpClient` WebSocket 客户端）
- SQLite + Spring JDBC（WAL，`busy_timeout`）

## 目录结构

```
qqChatBot/
├─ pom.xml
├─ .gitignore
├─ src/main/
│  ├─ java/com/komeowmeow/qqchatbot/
│  │  ├─ QqChatBotApplication.java
│  │  ├─ adapter/qq/                 # OneBot 连接、事件、消息解析与发送
│  │  │  ├─ QqClient.java            # WebSocket、API（get_msg / get_image）、发群消息
│  │  │  ├─ QqEventListener.java
│  │  │  ├─ GroupQuestionAssembler.java   # @ 检测、文本/CQ/回复消息组装
│  │  │  ├─ OneBotImageResolver.java      # 图片拉取与 Base64
│  │  │  └─ OneBotEvent.java
│  │  ├─ application/
│  │  │  ├─ ConversationService.java      # 按群串行、上下文、落库
│  │  │  ├─ MessageHandler.java           # System prompt + Qwen 调用（含超时）
│  │  │  ├─ ChatContextFormatter.java     # 历史条数/字数裁剪
│  │  │  ├─ ReplyErrorMessages.java       # 异常 → 群友好文案
│  │  │  ├─ UserQuestion.java
│  │  │  └─ ImageAttachment.java
│  │  ├─ config/
│  │  │  ├─ AiConfig.java
│  │  │  ├─ BotConfig.java
│  │  │  └─ BotLimitsProperties.java
│  │  └─ infrastructure/persistence/
│  │     └─ ChatMessageRepository.java
│  └─ resources/
│     ├─ application.yml
│     ├─ schema.sql
│     └─ prompts/
│        ├─ system-prompt.txt              # 当前使用的人设与规则
│        └─ system-prompt.full-quotes.txt  # 备用长版，未在代码中加载
└─ README.md
```

## 配置

密钥与连接信息优先用环境变量，不要写进仓库。

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key（**必填**，启动时未设置会抛错） | 无 |
| `DASHSCOPE_MODEL` | 模型名称 | `qwen3.7-flash` |
| `DASHSCOPE_REPLY_TIMEOUT_SECONDS` | 单次模型调用超时（秒） | `90` |
| `ONEBOT_WS_URL` | NapCat OneBot WebSocket 地址 | `ws://127.0.0.1:3001/` |
| `ONEBOT_ACCESS_TOKEN` | NapCat `access_token`，未配置可留空 | 空 |
| `BOT_CONTEXT_MAX_MESSAGES` | 带入 prompt 的历史条数上限（user/assistant 各算 1 条） | `10` |
| `BOT_CONTEXT_MAX_CHARS` | 历史上下文拼接后的最大字符数 | `8000` |
| `BOT_IMAGE_MAX_BYTES_PER_IMAGE` | 单张图片解码后最大字节数 | `5242880`（5 MiB） |
| `BOT_IMAGE_MAX_TOTAL_BYTES` | 同一条消息内多图合计最大字节数 | `8388608`（8 MiB） |

也可直接改 `application.yml`，环境变量会覆盖其中的默认值。

### `bot.qq`

| 项 | 说明 |
| --- | --- |
| `enabled` | 为 `false` 时不连接 NapCat |
| `self-id` | 机器人 QQ 号，用于过滤自身消息与识别 @ |
| `one-bot-web-socket-url` | 对应 `ONEBOT_WS_URL` |
| `access-token` | 对应 `ONEBOT_ACCESS_TOKEN` |

### `bot.limits`

| 项 | 环境变量 | 说明 |
| --- | --- | --- |
| `context-max-messages` | `BOT_CONTEXT_MAX_MESSAGES` | 从 SQLite 读取最近 N 条（代码内限制在 1～50） |
| `context-max-chars` | `BOT_CONTEXT_MAX_CHARS` | 超出则从最早的消息丢弃；单条过长会截断 |
| `image-max-bytes-per-image` | `BOT_IMAGE_MAX_BYTES_PER_IMAGE` | 超限图跳过；仅图且全部超限时提示「图太大」 |
| `image-max-total-bytes` | `BOT_IMAGE_MAX_TOTAL_BYTES` | 多图累计超限则跳过后续图 |

### `ai.dashscope`

| 项 | 环境变量 | 说明 |
| --- | --- | --- |
| `model` | `DASHSCOPE_MODEL` | 通义千问模型名 |
| `reply-timeout-seconds` | `DASHSCOPE_REPLY_TIMEOUT_SECONDS` | 超时后不发模型回复、不落库，并在群里提示 |

Spring Boot 会监听 **8080**（`server.port`），当前未提供对外 HTTP API，主要用于内嵌容器与配置加载。

## NapCat 配置示例

本仓库作为 **OneBot v11 客户端**，主动连接 NapCat 的 **正向 WebSocket 服务端**（`websocketServers`）。反向 WS（`websocketClients`）不适用。

### WebUI（推荐）

1. 打开 NapCat WebUI → **网络配置** → 新建 **WebSocket 服务端**。
2. 建议项（与 `application.yml` / 环境变量默认值对齐）：

| NapCat 项 | 建议值 | 说明 |
| --- | --- | --- |
| 启用 | 开 | |
| 监听地址 `host` | `0.0.0.0` 或 `127.0.0.1` | 本机部署用 `127.0.0.1` 即可 |
| 端口 `port` | `3001` | 对应 `ONEBOT_WS_URL` |
| `messagePostFormat` | **`array`** | 便于解析 `at` / `image` / `reply` 段；`string`（CQ 码）也支持，但回复引用里的图主要靠数组格式 |
| `token` | 可选 | 若填写，须与 `ONEBOT_ACCESS_TOKEN` 一致 |
| `reportSelfMessage` | 关 | 本应用会自行过滤 `self-id` |

3. 保存并启用后，本机连接地址一般为 `ws://127.0.0.1:3001/`（末尾斜杠有无均可）。

### `onebot11_<QQ>.json` 片段

配置文件路径以 NapCat 版本为准，常见于 `./config/onebot11_<机器人QQ>.json`。仅示意 **websocketServers** 一节：

```json
{
  "network": {
    "websocketServers": [
      {
        "name": "WsServer",
        "enable": true,
        "host": "127.0.0.1",
        "port": 3001,
        "messagePostFormat": "array",
        "reportSelfMessage": false,
        "token": "your-onebot-token",
        "debug": false,
        "heartInterval": 30000
      }
    ]
  }
}
```

### 与本项目的对应关系

| NapCat | 本项目 |
| --- | --- |
| 登录的机器人 QQ | `bot.qq.self-id`（`application.yml`） |
| WS 地址 | `ONEBOT_WS_URL` → `bot.qq.one-bot-web-socket-url` |
| `token` | `ONEBOT_ACCESS_TOKEN` → `bot.qq.access-token` |

鉴权方式：若配置了 `token`，`QqClient` 在建立 WebSocket 时发送请求头 `Authorization: Bearer <token>`（不是 query 里的 `access_token` 参数）。

### 会用到的 OneBot 动作

| 动作 | 用途 |
| --- | --- |
| 事件上报 `message`（群） | 接收 @ 与消息内容 |
| `send_group_msg` | 回复群消息 |
| `get_msg` | 回复引用时拉取被回复消息（含图） |
| `get_image` | 解析 CQ / 本地图床文件为可下载数据 |

连接成功时日志会出现：`已连接 NapCat：ws://...`。

## 运行

```bash
# 1. 启动 NapCat，并开启 OneBot v11 WebSocket
# 2. 配置 API Key
export DASHSCOPE_API_KEY=sk-xxx   # Linux / macOS

# Windows PowerShell
# $env:DASHSCOPE_API_KEY="sk-xxx"

# 3. 启动
mvn spring-boot:run
```

首次启动会根据 `schema.sql` 初始化表；数据库文件 `qq-chat-bot.db` 落在项目根目录，已在 `.gitignore` 中忽略。

## 数据库与表字段

### 文件与连接

| 项 | 值 |
| --- | --- |
| 文件路径 | 项目根目录 `qq-chat-bot.db`（相对 JVM 工作目录） |
| JDBC URL | `jdbc:sqlite:qq-chat-bot.db?busy_timeout=5000&journal_mode=WAL` |
| 建表脚本 | `src/main/resources/schema.sql`（`spring.sql.init.mode: always`，启动时执行） |
| 连接池 | HikariCP `maximum-pool-size: 1`（SQLite 单写者） |

### 表 `chat_message`

存储**已进入 AI 上下文**的问答片段：每次成功回复会插入 **2** 行（用户一条 + 助手一条）。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `INTEGER` PK 自增 | 单调递增；拉取「最近 N 条」按 `(group_id, id DESC)` 排序 |
| `group_id` | `INTEGER` NOT NULL | QQ 群号 |
| `user_id` | `INTEGER` NOT NULL | 触发本轮对话的群友 QQ 号；助手行也保留该 ID，表示这条回复对应的提问者 |
| `user_name` | `TEXT` NOT NULL | 收到消息时的群名片；群名片为空时使用 QQ 昵称。启动时会为旧数据库自动补列 |
| `role` | `TEXT` NOT NULL | `user`：用户提问；`assistant`：机器人回复 |
| `content` | `TEXT` NOT NULL | 文本内容；含图时用户行见下方说明 |
| `created_at` | `TEXT` | 默认 `CURRENT_TIMESTAMP`（SQLite 当前时间） |

**`role` 取值**（代码层校验，仅允许二者）：

- `user`：发给模型的用户侧内容（经 `UserQuestion.toStoredContent()` 落库）。
- `assistant`：通义千问返回的正文（trim 后）。

**`content` 约定**：

- 纯文字：与群里去掉 @ 后的有效文案一致；空图无字时在库里也是占位逻辑对应的「看看这张图」类文本。
- 含图：首行为有效文字，下面每有一张图追加一行 `[图片]`（图本身不入库，仅标记）。
- 助手回复：纯文本，无 `[图片]` 占位。

**索引**：

```sql
CREATE INDEX idx_chat_message_group_id ON chat_message(group_id, id);
```

用于按群查询最近记录。读取条数由 `bot.limits.context-max-messages` 决定（上限 **50**），再经 `context-max-chars` 裁剪，格式化为「群友(昵称: 群名片, ID: QQ号)：… / 你：…」拼进 prompt。另会附带数据库中最近活跃的最多 200 位已记录群友名册，让模型回答昵称问题；没有记录时要求模型明确说明，不猜测。当前提问也会标出提问者昵称和 QQ 号。

历史上下文中的提问会显示群名片（无群名片时显示 QQ 昵称）和 QQ 号，不同群友的发言可据此区分。直接查询数据库时，查看 `chat_message.user_name` 和 `chat_message.user_id` 即可识别提问者；助手回复行也保留对应提问者信息。旧记录的 `user_name` 会是空字符串，因为历史数据中没有保存昵称。

未成功调用 AI 的提问**不会**写入该表。

## 行为说明

- **群消息 only**：`post_type=message` 且 `message_type=group`；私聊忽略。
- **触发条件**：消息中须 @ 本机器人（`self-id`）；纯 @ 无文字、无图、无有效回复引用时不处理。
- **图片**：消息内 `[CQ:image]` / 数组段 `image` 均可；可 **回复** 某条消息并把被回复里的图一并送入模型（通过 `get_msg`）。单次最多 **3** 张图；受 `bot.limits` 单张/合计字节上限约束。全部解析失败 →「打不开」；仅图且全部超限 →「图太大」；有字且有合规图则照常调用模型。
- **仅文字为空但有图** 时，模型侧问题文案默认为「看看这张图」；落库时会在文字后附加 `[图片]` 标记。
- **并发**：`QqEventListener` 用虚拟线程异步处理；`ConversationService` 对同一群串行（64 路 stripe 锁），避免上下文与写入顺序错乱。
- **上下文**：条数/字数见 `bot.limits`；**AI 调用成功后再写入** 本轮问答。
- **超时与异常**：模型超过 `reply-timeout-seconds` 或处理链失败时，群里收简短提示（详见 `ReplyErrorMessages`），失败轮次不入库。
- **连接**：WebSocket 断线后指数退避重连（1s、2s、4s … 最长 30s）。

## 人设

回复风格由 `src/main/resources/prompts/system-prompt.txt` 定义，修改后重启生效。
