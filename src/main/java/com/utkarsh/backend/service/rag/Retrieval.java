package com.utkarsh.backend.service.rag;

import lombok.AllArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class Retrieval {
    private final VectorStore vectorStore;

    public List<Document> getTopKChunks(String query, UUID repoId ,int k) {
        Filter.Expression filter= new FilterExpressionBuilder()
                .eq("repoId", repoId.toString())
                .build();
        SearchRequest searchRequest = new SearchRequest.Builder()
                .query(query)
                .filterExpression(filter)
                .topK(k)
                .build();

        return vectorStore.similaritySearch(searchRequest);

    }
}
