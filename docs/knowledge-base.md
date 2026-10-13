# Agent 概念知识库（RAG 演示用）

本文件是两个模块共用的示例知识库。每个条目之间用水平分隔线隔开，便于切分。

## 什么是 RAG

RAG（Retrieval-Augmented Generation，检索增强生成）是把"检索系统"和"大模型生成"结合起来的技术：
先从外部知识库中检索与问题相关的文本片段，再把片段作为上下文拼进提示词交给大模型，
让模型"基于给定资料"回答，从而减少幻觉、让回答携带模型训练数据之外的私有知识。
RAG 包含两个阶段：离线的知识导入（文档解析、切分、向量化入库）和在线的检索生成（问题向量化、相似度检索、上下文增强、生成回答）。

---

## 什么是 Tool Calling（工具调用）

Tool Calling（也叫 Function Calling）是让大模型"间接执行代码"的机制：
开发者把 Java 方法以 JSON Schema 的形式（名称、描述、参数）注册给模型；
模型在对话中判断自己需要外部能力时，不会直接编造答案，而是返回一个"工具调用请求"（要调用哪个工具、参数是什么）；
框架负责真正执行方法并把结果回传给模型，模型再基于结果生成最终回答。
一次对话中模型可以发起多轮工具调用，这是 Agent"感知-思考-行动"循环的基础。

---

## 什么是 Agent

Agent（智能体）是能自主规划并执行任务的系统。与普通聊天的区别在于：
普通聊天是"一问一答"，而 Agent 拿到目标后，会自己拆解步骤、决定调用哪些工具、
根据工具返回结果调整下一步行动，循环往复直到完成任务。
一个最小 Agent 通常由四部分组成：模型（LLM，负责决策）、记忆（Memory，保存上下文）、
工具（Tools，负责与外部世界交互）、规划策略（Planning，如 ReAct 模式：
模型交替产出"思考 Thought"和"行动 Action"，观察 Action 结果后再思考，直到得出最终答案）。

---

## 什么是 ReAct

ReAct（Reasoning + Acting）是最经典的 Agent 推理模式：
把"推理"（分析当前情况、规划下一步）和"行动"（调用工具）交替进行——
Thought: 我需要先查订单状态 → Action: queryOrder(id=123) → Observation: 订单已发货 →
Thought: 用户问的是退款，已发货需先拦截 → Action: 无，直接回答。
Spring AI 和 LangChain4j 的工具调用机制底层都是 ReAct 思想：框架自动完成了这个循环，
开发者只需注册工具并发起请求。

---

## 什么是 Embedding（向量嵌入）

Embedding 是把文本转换成一串浮点数（向量）的技术，语义相近的文本在向量空间中距离也相近。
向量通常有几百到几千维（如 1024 维）。语义检索的原理：
把知识库文档和用户问题都通过 Embedding 模型转成向量，
用余弦相似度等度量找出与问题向量最接近的文档向量——这就是"相似度搜索"，
它让"没出现同一个关键词但意思相近"的内容也能被检索到。

---

## 什么是对话记忆 ChatMemory

LLM 本身是无状态的：每次调用都要把完整上下文重新发过去。
对话记忆（Chat Memory）就是框架帮你维护"之前说过什么"的机制，常见策略：
1. 消息窗口（Message Window）：只保留最近 N 条消息，超出丢弃——LangChain4j 的 MessageWindowChatMemory 和 Spring AI 的 MessageWindowChatMemory 都是此策略；
2. 令牌窗口：按 token 数而非条数裁剪；
3. 向量记忆：把历史对话向量化，只检索与当前问题相关的旧对话（如 Spring AI 的 VectorStoreChatMemoryAdvisor）。
多会话隔离通常用一个 sessionId / conversationId 作为键。

---

## Spring AI 与 LangChain4j 的定位

Spring AI 是 Spring 官方的 AI 应用框架，核心理念是把 Spring 的可移植性和模块化带入 AI 工程：
通过 starter 自动配置模型客户端，用 ChatClient + Advisor 链组织功能，与 Spring 生态（Boot、Data、Security）无缝集成。
LangChain4j 是框架中立的 Java LLM 应用库（灵感来自 Python 的 LangChain），
特色是声明式 AiServices 接口代理、丰富的模型/向量库适配（30+ 向量库）、以及独立的 agentic 模块。
两者都不是大模型本身，而是"对接各种大模型、组织 Agent 应用结构"的中间层，
核心概念（对话模型、嵌入模型、记忆、工具调用、向量库、RAG 管线）几乎一一对应，学会一个另一个很容易迁移。

---

## 提示词模板 Prompt Template

提示词模板是把提示词中易变的部分抽成占位符的做法，例如：
"请用{language}写一个{topic}的示例代码"。
好处：提示词可复用、可由非开发人员维护、避免字符串拼接错误。
LangChain4j 提供 PromptTemplate 类与 @UserMessage/@SystemMessage 注解模板；
Spring AI 提供 PromptTemplate 类，也支持 ChatClient 的 .user(u -> u.text("...{topic}").param("topic", "...")) 占位符用法。
