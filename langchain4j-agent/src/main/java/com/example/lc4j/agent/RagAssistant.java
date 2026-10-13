package com.example.lc4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * RAG 助手：装配时挂了 RetrievalAugmentor，框架会自动：
 *   把 question 向量化 -> 从向量库检索相似片段 -> 拼进提示词 -> 交给模型基于资料回答。
 *
 * 学习要点：
 * - 业务代码完全看不到"检索"这一步，它被 RetrievalAugmentor 透明地织入（与 Spring AI 的 Advisor 思路异曲同工）；
 * - {{userMessage}} 之外的 {{it}} / @V 变量可用于把外部数据注入模板（这里演示 @MemoryId 之外的可选写法）。
 */
public interface RagAssistant {

    @SystemMessage("""
            你是一名严谨的技术资料问答助手。
            优先依据【上下文】回答问题；若上下文没有相关信息，请明确说明你不知道，不要编造。
            """)
    @UserMessage("问题：{{question}}")
    String answer(@V("question") String question);
}
