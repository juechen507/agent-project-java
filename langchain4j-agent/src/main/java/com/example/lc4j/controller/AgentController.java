package com.example.lc4j.controller;

import com.example.lc4j.agent.ChatAssistant;
import com.example.lc4j.agent.RagAssistant;
import com.example.lc4j.agent.ToolAssistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

/**
 * Agent 能力演示接口（端口 8082）。每个接口对应一个核心概念，试法见 README。
 *
 * 与 spring-ai-agent 模块接口一一对应，方便对照两个框架实现同一件事的写法差异。
 */
@RestController
@RequestMapping("/api")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final ChatAssistant chatAssistant;
    private final ToolAssistant toolAssistant;
    private final RagAssistant ragAssistant;
    private final StreamingChatModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public AgentController(ChatAssistant chatAssistant, ToolAssistant toolAssistant,
                           RagAssistant ragAssistant, StreamingChatModel streamingChatModel,
                           EmbeddingModel embeddingModel, InMemoryEmbeddingStore<TextSegment> embeddingStore) {
        this.chatAssistant = chatAssistant;
        this.toolAssistant = toolAssistant;
        this.ragAssistant = ragAssistant;
        this.streamingChatModel = streamingChatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /** 概念1：基础对话（用固定会话键 "default"，其实也会累积记忆） */
    @GetMapping("/chat")
    /*
        实际执行调用的是proxy对象，具体逻辑见DefaultAiServices...invoke(..) 方法
     */
    public String chat(@RequestParam("message") String message) {
        return chatAssistant.chat("default", message);
    }

    /** 概念2：多轮对话记忆 —— sessionId 相同即共享同一段 ChatMemory */
    @GetMapping("/chat/memory")
    public String chatWithMemory(@RequestParam("sessionId") String sessionId,
                                 @RequestParam("message") String message) {
        return chatAssistant.chat(sessionId, message);
    }

    /** 概念3：流式输出 —— 用 StreamingChatModel 把回调桥接成 SSE（Flux）逐 token 推送 */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam("message") String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        streamingChatModel.chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partial) {
                sink.tryEmitNext(partial);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式对话出错", error);
                sink.tryEmitError(error);
            }
        });
        return sink.asFlux();
    }

    /** 概念4：工具调用 —— ToolAssistant 装配时注册了工具，模型按需调用 */
    @GetMapping("/chat/tool")
    public String chatWithTools(@RequestParam("sessionId") String sessionId,
                                @RequestParam("message") String message) {
        return toolAssistant.chat(sessionId, message);
    }

    /** 概念5：RAG —— RagAssistant 挂了 RetrievalAugmentor，自动检索知识库再作答 */
    @GetMapping("/rag")
    public String rag(@RequestParam("question") String question) {
        return ragAssistant.answer(question);
    }

    /** 调试利器：跳过 LLM，直接看向量检索命中了哪些文本块 */
    @GetMapping("/search")
    public List<String> search(@RequestParam("q") String q,
                               @RequestParam(value = "top-k", defaultValue = "4") int topK) {
        var queryEmbedding = embeddingModel.embed(q).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(topK)
                        .minScore(0.5)
                        .build()
        ).matches();
        return matches.stream()
                .map(m -> "[score=%.3f] %s".formatted(m.score(), m.embedded().text()))
                .toList();
    }
}
