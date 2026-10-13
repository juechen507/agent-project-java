package com.example.lc4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 知识库导入（RAG 的 Data Ingestion 阶段）—— LangChain4j 版，启动时执行一次。
 *
 * 学习要点：
 * 这里用框架提供的「一站式」工具 EmbeddingStoreIngestor，它把三步流水线打包好了：
 *   Document（文档） --splitter 切分--> TextSegment（片段） --embeddingModel 向量化--> 存入 embeddingStore
 * 对照 spring-ai-agent 模块里手工 read->split->add 的写法，可以看出两种框架封装粒度不同的设计取向。
 *
 * 本例读取与 Spring AI 模块共用的 docs/knowledge-base.md（按 --- 分隔为多个条目）。
 * 为简单起见把整篇读成一个 Document，再交给递归切分器按长度切块。
 */
@Component
public class KnowledgeBaseIngester implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngester.class);

    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeBaseIngester(EmbeddingModel embeddingModel,
                                 InMemoryEmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String markdown;
        try (InputStream in = new ClassPathResource("data/knowledge-base.md").getInputStream()) {
            markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50)) // 每块约 500 字符，重叠 50
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(Document.from(markdown));
        log.info("知识库导入完成：已向量化并写入 InMemoryEmbeddingStore");
    }
}
