package com.example.lc4j.config;

import com.example.lc4j.agent.ChatAssistant;
import com.example.lc4j.agent.RagAssistant;
import com.example.lc4j.agent.ToolAssistant;
import com.example.lc4j.agent.AgentTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用 AiServices 把接口 + 各种能力"装配"成可注入的 Bean —— LangChain4j 的核心用法。
 *
 * 学习要点：
 * - AiServices.builder(接口.class) 链式声明：挂哪个模型、要不要记忆、要不要工具、要不要 RAG；
 * - chatMemoryProvider(id -> ...) 为每个 memoryId 惰性创建一段独立记忆（默认消息窗口 20 条）；
 * - 三个助手演示三种能力组合，接口几乎不变、差异全在装配 —— 体会"关注点分离"。
 */
@Configuration
public class AgentServicesConfig {

    /** 能力1+2：基础多轮对话（带记忆） */
    @Bean
    public ChatAssistant chatAssistant(ChatModel chatModel) {
        return AiServices.builder(ChatAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20)
                                .build())
                .build();
    }

    /** 能力3：工具调用（在记忆之上再注册工具对象） */
    @Bean
    public ToolAssistant toolAssistant(ChatModel chatModel, AgentTools tools) {
        return AiServices.builder(ToolAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .tools(tools)
                .build();
    }

    /** 能力4：RAG —— 挂上 RetrievalAugmentor，检索对用户透明 */
    @Bean
    public RagAssistant ragAssistant(ChatModel chatModel, RetrievalAugmentor retrievalAugmentor) {
        return AiServices.builder(RagAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }
}
