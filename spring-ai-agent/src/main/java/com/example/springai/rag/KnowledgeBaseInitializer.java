package com.example.springai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库导入（RAG 的 Data Ingestion 阶段），应用启动时执行一次。
 *
 * 学习要点 —— 三步走：
 * 1. Read    ：MarkdownDocumentReader 把文档解析成 Document（文本 + 元数据）；
 * 2. Split    ：TokenTextSplitter 按 token 数切分成小块 —— 切分策略直接影响检索质量，是 RAG 调优第一杠杆；
 * 3. Embed+Store：vectorStore.add() 时内部调用 EmbeddingModel 把每块文本转成向量存入。
 *
 * 对比 LangChain4j：等价组合是 DocumentLoader + DocumentSplitter + EmbeddingModel + EmbeddingStoreIngestor（本项目的
 * langchain4j-agent 模块用一行 Ingestor 做了同样的事，方便对照）。
 */
@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    private final VectorStore vectorStore;

    public KnowledgeBaseInitializer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        MarkdownDocumentReader reader = new MarkdownDocumentReader(
                new ClassPathResource("data/knowledge-base.md"),
                MarkdownDocumentReaderConfig.builder()
                        // 遇到水平分隔线就切成独立文档，让每个知识条目天然成为一个块
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .build());
        List<Document> documents = reader.get();

        List<Document> chunks = new TokenTextSplitter().apply(documents);

        vectorStore.add(chunks);
        log.info("知识库导入完成：{} 个文档 -> {} 个分块，已写入向量库", documents.size(), chunks.size());
    }
}
