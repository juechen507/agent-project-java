package com.example.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Agent 学习示例入口（端口 8081）
 *
 * 学习要点 —— Spring AI 核心概念：
 * - ChatClient        fluent API，是调用大模型的主要入口（对比 LangChain4j 的 AiServices）
 * - Advisor           拦截器链，给对话增强记忆/RAG 等能力（对比 LangChain4j 的 ChatMemory/RetrievalAugmentor）
 * - @Tool             注解方法暴露给模型调用（对比 LangChain4j 的 @Tool）
 * - ChatMemory        对话记忆，按 conversationId 隔离（对比 LangChain4j 的 ChatMemory）
 * - VectorStore       向量存储抽象（对比 LangChain4j 的 EmbeddingStore）
 */
@SpringBootApplication
public class SpringAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiAgentApplication.class, args);
    }
}
