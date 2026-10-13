package com.example.lc4j.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 手动装配 LangChain4j 的模型 Bean，并把 application.yml 里的 API 配置项逐个应用上去。
 *
 * 学习要点：
 * - LangChain4j 是框架中立的库，本身不做自动配置（若要自动配置需另引 langchain4j-open-ai-spring-boot-starter）。
 *   这里用 @Bean 手工 new，最能看清「一个模型 = base-url + api-key + model-name + 生成参数」的本质。
 * - ChatModel 一次性返回；StreamingChatModel 逐 token 回调；EmbeddingModel 负责文本向量化（供 RAG 使用）。
 * - baseUrl 指向 OpenAI 兼容端点（示例默认 DashScope 兼容模式，注意末尾要带 /v1）。
 *   langchain4j VS spring.ai
 *   dev.langchain4j.model.openai.internal.DefaultOpenAiClient VS org.springframework.ai.openai.api.OpenAiApi
 * - 所有可调项集中在 agent.* 配置，改配置不改代码。
 */
@Configuration
public class AgentModelConfig {

    @Value("${agent.api-key}")
    private String apiKey;

    /*
        Spring Boot 读取 ${agent.base-url}` 时，Environment的属性源按优先级从高到低查找
        环境变量的优先级（模糊匹配AGENT_BASE_URL）要高于 application.yml 的值，
        即使 application.yml 里配置了，仍然不生效，故这里修改了变量名
        （此项目因为要与spring.ai共用 .env 文件，所以才这么“另类”）
     */
    @Value("${agent.base-url2}")
    private String baseUrl;

    @Value("${agent.chat-model}")
    private String chatModelName;

    @Value("${agent.temperature:0.7}")
    private double temperature;

    @Value("${agent.top-p:1.0}")
    private double topP;

    @Value("${agent.max-tokens:2048}")
    private int maxTokens;

    @Value("${agent.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${agent.max-retries:2}")
    private int maxRetries;

    @Value("${agent.embedding-model}")
    private String embeddingModelName;

    @Value("${agent.embedding-dimension:1024}")
    private int embeddingDimension;

    @Value("${agent.log-requests:true}")
    private boolean logRequests;

    @Value("${agent.log-responses:true}")
    private boolean logResponses;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(temperature)
                .topP(topP)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    /** 流式对话模型：/chat/stream 接口用它演示逐 token 输出 */
    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(temperature)
                .topP(topP)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    /**
     * 嵌入模型：RAG 把知识分块和问题都转成向量时用它。
     * dimensions 需与向量库/模型输出维度一致（DashScope text-embedding-v3 默认 1024）。
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .dimensions(embeddingDimension)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .build();
    }
}
