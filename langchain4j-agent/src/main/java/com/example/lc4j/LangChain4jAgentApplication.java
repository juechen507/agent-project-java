package com.example.lc4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LangChain4j Agent 学习示例入口（端口 8082）。
 *
 * 学习要点 —— LangChain4j 核心概念（与 Spring AI 对照）：
 * - ChatModel / StreamingChatModel  对话模型抽象（对比 Spring AI 的 ChatModel）
 * - AiServices + 接口注解            声明式 AI 服务，定义接口即得实现（对比 Spring AI 的 ChatClient 编程式）
 * - @Tool                            工具方法注解（对比 Spring AI 的 @Tool）
 * - ChatMemory / ChatMemoryProvider  对话记忆（对比 Spring AI 的 ChatMemory + Advisor）
 * - EmbeddingModel / EmbeddingStore  嵌入与向量存储（对比 Spring AI 的 EmbeddingModel / VectorStore）
 * - RetrievalAugmentor               RAG 检索增强器（对比 Spring AI 的 RetrievalAugmentationAdvisor）
 */
@SpringBootApplication
public class LangChain4jAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jAgentApplication.class, args);
    }
}
