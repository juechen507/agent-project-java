package com.example.lc4j.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 声明式 AI 服务 —— LangChain4j 最独特的抽象（对比 Spring AI 的编程式 ChatClient）。
 *
 * 学习要点：
 * - 你只定义接口 + 注解，框架用动态代理生成实现（AiServices.builder 装配后 build 出实例）；
 * - @SystemMessage 固定系统提示词；@UserMessage 标记用户输入参数；
 * - @MemoryId 参数作为会话键：相同 memoryId 共享同一段 ChatMemory，实现多轮记忆。
 */
public interface ChatAssistant {

    @SystemMessage("你是一个用于学习 Agent 概念的 Java 助手，回答要简洁。")
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
