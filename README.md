# Agent 学习项目：Spring AI vs LangChain4j

这是一个**用于学习**的 Java Agent 项目，用两个当前主流的 Java 大模型框架各搭建一套功能等价的最小实现，
方便你**横向对比**：同一组核心概念（对话、记忆、工具调用、RAG、流式输出）在两个框架里分别怎么写、抽象叫什么名字。

> 项目定位是"看懂概念"，不是"生产可用"。向量库、记忆都用内存实现，重启即清空。

## 模块结构

```
agent-project-java/                  父工程（统一管理版本）
├── .env.example                     API 配置项模板（复制成 .env 填 Key）
├── docs/knowledge-base.md           两个模块共用的 RAG 示例知识库
├── spring-ai-agent/                 Spring AI 实现（端口 8081）
│   └── src/main/java/com/example/springai/
│       ├── SpringAiAgentApplication  启动类
│       ├── config/AgentConfig        ChatClient + 对话记忆（Advisor 链）
│       ├── config/RagConfig          向量库 + 两种 RAG Advisor
│       ├── agent/AgentTools          @Tool 工具方法
│       ├── rag/KnowledgeBaseInitializer  知识库导入（手工 read→split→add）
│       └── controller/AgentController  REST 接口（每个概念一个端点）
└── langchain4j-agent/               LangChain4j 实现（端口 8082）
    └── src/main/java/com/example/lc4j/
        ├── LangChain4jAgentApplication  启动类
        ├── config/AgentModelConfig      手写 @Bean 装配三种模型（读取 agent.* 配置）
        ├── config/AgentServicesConfig   AiServices 装配三个助手
        ├── config/RagConfig             向量库 + RetrievalAugmentor
        ├── agent/ChatAssistant|ToolAssistant|RagAssistant  声明式 AI 服务接口
        ├── agent/AgentTools            @Tool 工具方法
        ├── rag/KnowledgeBaseIngester   知识库导入（EmbeddingStoreIngestor 一站式）
        └── controller/AgentController  REST 接口
```

## 环境要求

- JDK 17+（本项目按 JDK 21 编译）
- Maven 3.8+
- 一个 **OpenAI 兼容** 的大模型 API Key（默认配置指向阿里云 DashScope 兼容模式，也可换 DeepSeek 等）

## 配置说明（API 相关配置项）

所有 API 配置项集中在两个地方，二选一即可：

1. **推荐：用 `.env` 环境变量**
   ```bash
   cp .env.example .env      # 复制模板
   # 编辑 .env 填入 AGENT_API_KEY 等
   set -a && source .env && set +a   # 加载到当前 shell
   ```
2. **或直接改 `application.yml` 默认值**：每个配置项都是 `${环境变量:默认值}` 形式，把冒号后的默认值改成你的值即可。

主要配置项（两边语义一致）：

| 配置项 | 环境变量 | 含义 |
|--------|----------|------|
| base-url | `AGENT_BASE_URL` | OpenAI 兼容服务地址（Spring AI 侧不带 `/v1`，LangChain4j 侧需带 `/v1`） |
| api-key | `AGENT_API_KEY` | 密钥（必填） |
| chat model | `AGENT_CHAT_MODEL` | 对话模型名，如 `qwen-plus` |
| embedding model | `AGENT_EMBEDDING_MODEL` | 向量模型名，如 `text-embedding-v3` |
| embedding dim | `AGENT_EMBEDDING_DIM` | 向量维度，需与服务端一致（默认 1024） |
| temperature | `AGENT_TEMPERATURE` | 采样温度 0~2 |
| top-p | `AGENT_TOP_P` | 核采样 |
| max-tokens | `AGENT_MAX_TOKENS` | 单次回复最大 token |
| timeout | `AGENT_TIMEOUT_SECONDS` | 请求超时秒数（LangChain4j 侧生效） |
| log requests/responses | `AGENT_LOG_REQUESTS` / `AGENT_LOG_RESPONSES` | 打印原始请求响应（LangChain4j 侧生效） |

## 快速开始

### 1. 编译

```bash
mvn -B compile
```

### 2. 分别启动两个模块（建议开两个终端，且已 source .env）

```bash
mvn -pl spring-ai-agent     spring-boot:run     # http://localhost:8081
mvn -pl langchain4j-agent   spring-boot:run     # http://localhost:8082
```

### 3. 逐个概念试用（两框架端点一一对应）

| 概念 | Spring AI (8081) | LangChain4j (8082) |
|------|------------------|--------------------|
| 基础对话 | `GET /api/chat?message=你好` | `GET /api/chat?message=你好` |
| 多轮记忆 | `GET /api/chat/memory?sessionId=u1&message=我叫小明` → 再问 `...&message=我叫什么` | `GET /api/chat/memory?sessionId=u1&message=我叫小明` → 再问 |
| 流式输出 | `GET /api/chat/stream?message=讲个笑话`（SSE） | `GET /api/chat/stream?message=讲个笑话`（SSE） |
| 工具调用 | `GET /api/chat/tool?message=现在几点了` | `GET /api/chat/tool?sessionId=t1&message=128乘以7等于几` |
| RAG 问答 | `GET /api/rag/qa?question=什么是RAG`（简单版）<br>`GET /api/rag/pipeline?question=什么是ReAct`（标准管线版） | `GET /api/rag?question=什么是RAG` |
| 只看检索结果 | `GET /api/search?q=向量嵌入` | `GET /api/search?q=向量嵌入` |

工具调用建议开 `DEBUG` 日志观察：模型先返回"要调用哪个工具"，框架执行后把结果回传，模型再组织最终回答（ReAct 循环）。

## 核心概念对照表（重点）

两个框架的**抽象几乎一一对应**，学会一个，另一个看代码就能迁移：

| 概念 / 职责 | Spring AI | LangChain4j |
|------------|-----------|-------------|
| 对话模型接口 | `ChatModel` | `ChatModel` |
| 流式模型接口 | `ChatModel.stream()` | `StreamingChatModel` |
| 高层调用门面 | **`ChatClient`**（编程式 fluent API） | **`AiServices` + 接口注解**（声明式代理） |
| 系统/用户提示词 | `.defaultSystem()` / `.user()` | `@SystemMessage` / `@UserMessage` |
| 横切增强机制 | **`Advisor`**（拦截器链） | 通过 `AiServices` 装配各组件 |
| 对话记忆 | `ChatMemory` + `MessageChatMemoryAdvisor` | `ChatMemory` + `ChatMemoryProvider`（`@MemoryId`） |
| 会话隔离键 | `chat_memory_conversation_id` 参数 | `@MemoryId` 方法参数 |
| 工具定义 | `@Tool` / `@ToolParam` | `@Tool` / `@P` |
| 工具注册 | `.tools(obj)` | `AiServices...tools(obj)` |
| 嵌入模型 | `EmbeddingModel` | `EmbeddingModel` |
| 向量存储抽象 | `VectorStore`（`SimpleVectorStore`） | `EmbeddingStore`（`InMemoryEmbeddingStore`） |
| 文档 | `Document` | `Document` / `TextSegment` |
| 文本切分 | `TokenTextSplitter` | `DocumentSplitters.recursive(...)` |
| RAG 检索器 | `VectorStoreDocumentRetriever` | `EmbeddingStoreContentRetriever` |
| RAG 编排 | `RetrievalAugmentationAdvisor` / `QuestionAnswerAdvisor` | `DefaultRetrievalAugmentor` |
| 知识导入 | 手工 `read→split→vectorStore.add` | `EmbeddingStoreIngestor` 一站式 |
| 模型接入方式 | starter **自动配置**（`spring.ai.*`） | 手写 `@Bean`（框架中立，可选 spring-boot-starter） |

## 两个框架的设计差异（学习要点）

1. **编程式 vs 声明式**
   - Spring AI 以一个 `ChatClient` 为中心，用链式 API 在运行时决定挂哪些 Advisor、传哪些工具；
   - LangChain4j 更"魔法"：你写一个**接口**加注解，`AiServices` 用动态代理生成实现，像写普通 Java 接口一样调用 AI。

2. **能力如何组合**
   - Spring AI 的 `Advisor` 是统一的环绕拦截链（记忆、RAG、日志都是 Advisor），概念收敛；
   - LangChain4j 把 `ChatMemory` / `RetrievalAugmentor` / `tools` 作为独立零件，在 `AiServices.builder` 处组装，概念更"显式分散"。

3. **与 Spring 生态的整合深浅**
   - Spring AI 原生依赖注入 + 自动配置，配 `application.yml` 即起；
   - LangChain4j 框架中立，本模块**故意手写 `@Bean`** 让你看清"一个模型 = baseUrl + apiKey + modelName + 生成参数"，
     实际项目也可改用 `langchain4j-*-spring-boot-starter` 走自动配置。

4. **RAG 封装粒度**
   - 看两个模块的"知识导入"就能对比：Spring AI 侧手工三步、LangChain4j 侧一行 `Ingestor`，
     直观感受两种框架"暴露细节 vs 打包便利"的取舍。

## 建议的学习路径

1. 先跑通"基础对话"，打开 `DEBUG` 日志看请求/响应长什么样；
2. 玩"多轮记忆"，体会 `sessionId` 如何隔离上下文，理解"LLM 本身无状态"；
3. 玩"工具调用"，重点观察日志里的 **两次模型往返**（决定调用 → 拿到结果再答），这是 Agent 的基石；
4. 用 `/api/search` 先**只看检索**命中了什么，再对比 `/api/rag` 让模型基于检索作答，理解 RAG 全链路；
5. 最后打开两边源码，对照上面的表格，把概念"翻译"一遍。

## 扩展方向（学完基础后再做）

- 把内存向量库换成 Milvus / PgVector（两边都只需换 `VectorStore`/`EmbeddingStore` 实现）；
- 接入 **MCP** 工具、实现真正的多步 ReAct Agent（LangChain4j 有 `langchain4j-agentic` 模块）；
- 记忆持久化到数据库（Spring AI 的 `JdbcChatMemoryRepository` 等）。

## 常见问题

- **启动就报知识库/embedding 错误**：多半是 `AGENT_EMBEDDING_MODEL` 或维度与服务端不匹配；DashScope `text-embedding-v3` 默认 1024 维。
- **401/鉴权失败**：确认 `AGENT_API_KEY` 已 `export` 且在启动的那个 shell 里生效。
- **工具没被调用**：换更明确的提问（"现在几点了""帮我算 128*7"），并确认日志级别为 DEBUG 观察往返。
