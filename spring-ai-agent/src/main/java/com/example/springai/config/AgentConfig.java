package com.example.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 与对话记忆配置。
 *
 * 学习要点：
 * - ChatClient 是 Spring AI 的核心门面（fluent API），底层委托 ChatModel 调用具体模型；
 * - Advisor 是环绕对话的拦截器链：日志、记忆、RAG 都以 Advisor 形式插拔 —— 这是 Spring AI 最有特色的抽象；
 * - ChatMemory 默认使用消息窗口策略，只保留最近 N 条，按 conversationId 区分会话；
 * - Spring AI 没有 LangChain4j 那样的 @AiService 声明式接口，而是统一用 ChatClient.Builder 编程式构建。
 */
@Configuration
public class AgentConfig {

    private static final String SYSTEM_PROMPT = """
            你是一个用于学习 Agent 概念的 Java 助手。
            回答要简洁。当需要时间、数学计算等信息时，优先调用工具获取真实结果，不要凭空编造。
            """;

    /**
     * 对话记忆：内存版消息窗口（重启即丢失，学习用途足够；生产中可换 JdbcChatMemoryRepository 等持久化实现）
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /**
     * 通用对话 ChatClient：
     * - defaultSystem       设置系统提示词
     * - LoggerAdvisor       打印请求/响应，方便学习时观察底层数据流
     * - MessageChatMemoryAdvisor 自动把历史消息拼进上下文，实现多轮对话
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
