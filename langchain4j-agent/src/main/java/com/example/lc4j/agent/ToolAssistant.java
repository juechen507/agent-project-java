package com.example.lc4j.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 带工具调用能力的助手：在 ChatAssistant 基础上，装配时额外注册了 AgentTools。
 * 接口本身与 ChatAssistant 几乎相同 —— 差别全在 AiServices 的 .tools(...) 装配上，
 * 这正说明「能力是装配出来的，而不是写死在业务代码里」。
 */
public interface ToolAssistant {

    @SystemMessage("""
            你是一个用于学习 Agent 概念的 Java 助手。回答要简洁。
            当需要时间、日期或精确数学计算时，必须调用工具获取真实结果，不要凭空编造。
            """)
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
