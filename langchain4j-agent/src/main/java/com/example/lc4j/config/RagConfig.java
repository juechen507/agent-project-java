package com.example.lc4j.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 组件配置 —— 向量库 + 检索增强器。
 *
 * 学习要点（与 Spring AI 对照）：
 * - InMemoryEmbeddingStore<TextSegment>  ≈ Spring AI 的 SimpleVectorStore（内存实现，学习用零部署）；
 * - EmbeddingStoreContentRetriever       ≈ Spring AI 的 VectorStoreDocumentRetriever（相似度检索）；
 * - DefaultRetrievalAugmentor            ≈ Spring AI 的 RetrievalAugmentationAdvisor（模块化 RAG 管线）。
 *
 * 生产替换向量库时，只需把 InMemoryEmbeddingStore 换成 Milvus/PgVector 等 EmbeddingStore 实现，
 * 其余装配代码不动 —— 面向接口编程的好处。
 */
@Configuration
public class RagConfig {

    /** 向量库（泛型 TextSegment 表示命中的原始文本会被一并返回） */
    @Bean
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /** 检索增强器：内部用「嵌入模型 + 向量库」做 topK 相似度检索 */
    @Bean
    public RetrievalAugmentor retrievalAugmentor(EmbeddingModel embeddingModel,
                                                 InMemoryEmbeddingStore<TextSegment> embeddingStore) {
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(4)
                .minScore(0.5)
                .build();
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();
    }
}
