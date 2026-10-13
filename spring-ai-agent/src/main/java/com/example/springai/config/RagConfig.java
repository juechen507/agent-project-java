package com.example.springai.config;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG（检索增强生成）相关配置。
 *
 * 学习要点 —— RAG 标准管线（两框架概念一一对应）：
 *   文档 -> 切分(Splitter) -> 向量化(EmbeddingModel) -> 存入向量库(VectorStore/EmbeddingStore)
 *   提问 -> 相似度检索(Retriever) -> 命中文档拼进提示词(Augmenter) -> 模型生成回答
 *
 * Spring AI 提供了两种接入方式（本模块都演示）：
 * 1. QuestionAnswerAdvisor        —— 简单粗暴，一个 Advisor 搞定「检索+拼接」，适合快速上手；
 * 2. RetrievalAugmentationAdvisor —— 按 RAG 论文的标准模块化管线（Pre-Retrieval/Retrieval/Post-Retrieval/Generation），
 *    每个环节可单独替换，适合理解 RAG 内部结构。
 *    （对比 LangChain4j：等价物是 RetrievalAugmentor，含 QueryTransformer/ContentRetriever/ContentAggregator 等组件）
 */
@Configuration
public class RagConfig {

    /**
     * 向量库：使用内置内存实现 SimpleVectorStore，学习用途零部署成本。
     * 生产中可换成 Milvus / PgVector / Qdrant 等（只需换依赖和 Bean，其余代码不动 —— 这就是抽象的价值）。
     */
    @Bean
    /*
        EmbeddingModel 依赖 spring-ai-starter-model-openai 自动注入
        逻辑：spring-ai-starter-model-openai
            -> spring-ai-autoconfigure-model-openai
            -> META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
            -> OpenAiEmbeddingAutoConfiguration

    */
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /** 简单版 RAG Advisor：检索到的文档会自动填充进默认模板的 {question_answer_context} 占位符 */
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .build();
    }

    /** 标准管线版 RAG Advisor：显式组装「检索器 + 查询增强器」，其它环节使用默认组件 */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.5)
                        .topK(4)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true) // 检索不到时仍允许模型自由回答（也可关掉，改为直接返回"不知道"）
                        .build())
                .build();
    }
}
