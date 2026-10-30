# qqChatBot
使用langchain4j做了个qq聊天机器人



项目结构：

```java
qqChatBot/
├─ pom.xml
├─ README.md
├─ .gitignore
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/yourname/qqchatbot/
│  │  │     ├─ QqChatBotApplication.java
│  │  │     ├─ config/	//加载机器人和模型配置。密钥应通过环境变量或本地未提交配置注入，不能提交到 Git。
│  │  │     │  ├─ AiConfig.java
│  │  │     │  └─ BotConfig.java
│  │  │     ├─ adapter/
│  │  │     │  └─ qq/			//处理 QQ 平台的连接、事件接收和消息发送。这里先定义接口和平台 DTO，具体实现等选定 QQ 接入方案后再写。
│  │  │     │     ├─ QqClient.java
│  │  │     │     ├─ QqEventListener.java
│  │  │     │     └─ dto/
│  │  │     ├─ application/			//编排业务流程，例如判断是否 @ 机器人、整理上下文、调用 AI、回复群聊。
│  │  │     │  ├─ MessageHandler.java
│  │  │     │  └─ ConversationService.java
│  │  │     ├─ domain/		//放平台无关的核心模型，例如消息、会话和发言者。
│  │  │     │  ├─ message/
│  │  │     │  └─ conversation/
│  │  │     └─ infrastructure/
│  │  │        ├─ ai/				//封装 LangChain4j 和具体模型服务，避免业务代码直接依赖某个模型供应商。
│  │  │        │  └─ LangChain4jChatService.java
│  │  │        └─ persistence/
│  │  └─ resources/
│  │     ├─ application.yml
│  │     └─ prompts/				//存放系统提示词，方便调整机器人性格和回复规则。
│  │        └─ system-prompt.txt
│  └─ test/
│     └─ java/
└─ docs/
   └─ architecture.md
```

