package com.example.springai.controller;

import com.example.springai.agent.AgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Agent 能力演示接口（端口 8081）。每个接口对应一个核心概念。
 *
 * 试法见 README，例如：
 *   GET /api/chat?message=你好
 *   GET /api/chat/memory?sessionId=u1&message=我叫小明   再问   GET /api/chat/memory?sessionId=u1&message=我叫什么
 *   GET /api/chat/tool?message=现在几点了
 *   GET /api/rag/qa?question=什么是RAG
 *   GET /api/search?q=RAG          （跳过 LLM，直接看向量库检索命中了什么）
 */
@RestController
@RequestMapping("/api")
public class AgentController {

    private final ChatClient chatClient;
    private final AgentTools agentTools;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final VectorStore vectorStore;

    public AgentController(ChatClient chatClient, AgentTools agentTools,
                           QuestionAnswerAdvisor questionAnswerAdvisor,
                           RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                           VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.agentTools = agentTools;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.vectorStore = vectorStore;
    }

    /** 概念1：基础对话 —— ChatClient 最小用法（注意：默认挂上了记忆 Advisor，单轮测试也会进记忆） */
    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        return chatClient.prompt()
                .user(message)
                // must contain parameter: conversionId
                .advisors(a -> a.param(CONVERSATION_ID, UUID.randomUUID().toString()))
                .call()
                .content();
    }

    /** 概念2：多轮对话记忆 —— 通过 chat_memory_conversation_id 参数区分会话 */
    /*
        agent会将多轮信息均发送给LLM，所以即使是一次访问LLM的请求，其实包含了多轮提交的信息，
        LLM可以看到多条Message，故而看似有记忆
     */
    @GetMapping("/chat/memory")
    public String chatWithMemory(@RequestParam("sessionId") String sessionId, @RequestParam("message") String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    /** 概念3：流式输出 —— SSE 逐 token 返回，聊天产品标配（.call() 一次性，.stream() 流式） */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    /*
        stream() 流式，基于project reactor实现（响应式编程，非通常的命令式编程）
        建议先学习project reactor，再看这里的内部实现，思路就比较清晰了
     */
    public Flux<String> chatStream(@RequestParam("message") String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, UUID.randomUUID().toString()))
                .stream()
                .content();
    }

    /** 概念4：工具调用 —— .tools() 把 @Tool 方法交给模型，模型自主决定是否调用（ReAct 的基础） */
    /*
        message + tool 一起发送到LLM，LLM决定是否调用Tool，“是否调用”会显示在LLM的response中，
        spring.ai代码，通过判断response是否要调用tool（反射调用，前期已经将Tool解析后的信息，保存在prompt中）
        tool执行见方法，methodToolCallback.callMethod()
     */
    @GetMapping("/chat/tool")
    public String chatWithTools(@RequestParam("message") String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(CONVERSATION_ID, UUID.randomUUID().toString()))
                .tools(agentTools)
                .call()
                .content();
    }

    /** 概念5：RAG 简单版 —— QuestionAnswerAdvisor 自动检索向量库并拼进提示词 */
    @GetMapping("/rag/qa")
    /*
        0.事前加载“知识库”（计算每个文档的文本向量）
        1.调用OpenAiEmbeddingModel.call(..)，获得用户question的文本向量
        2.计算余弦相似度，取topN
        3.将从vectorStore 中取出的Document 附加到prompt.userMessage 上
        4.继续执行advisorChain
        ...
        6.执行ChatModelCallAdvisor.call(..)，由LLM决定使用哪一个Document
        ...
     */
    public String ragSimple(@RequestParam("question") String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(CONVERSATION_ID, UUID.randomUUID().toString())
                        .advisors(questionAnswerAdvisor))
                .call()
                .content();
    }

    /** 概念6：RAG 标准管线版 —— RetrievalAugmentationAdvisor（检索器/增强器可插拔） */
    /*
        RetrievalAugmentationAdvisor 的实现细节更多 than QuestionAnswerAdvisor
        见方法，before(..)
     */
    @GetMapping("/rag/pipeline")
    public String ragPipeline(@RequestParam("question") String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(CONVERSATION_ID, UUID.randomUUID().toString())
                        .advisors(retrievalAugmentationAdvisor))
                .call()
                .content();
    }

    /** 调试利器：不发 LLM，直接看向量相似度检索命中了哪些文本块（LangChain4j 侧有对应接口） */
    @GetMapping("/search")
    public Object search(@RequestParam("q") String q,
                         @RequestParam(name = "top-k", defaultValue = "4") int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(q)
                .topK(topK)
                .build())
                .stream()
                .map(d -> d.getText())
                .toList();
    }

    @GetMapping("health")
    public String health() {
        return "ok";
    }
}
