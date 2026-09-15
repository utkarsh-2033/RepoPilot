package com.utkarsh.backend.service.indexing;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FileChunking {

    @Autowired
    private TokenTextSplitter tokenTextSplitter;
    @Autowired
    private FileFilter fileFilter;

    public List<Document> chunkFile(String path , String content, UUID repoId){
        String language = fileFilter.detectLanguage(path);
        String header = "// File: " + path + "\n";

        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("repoId", repoId);
        metadataMap.put("filePath", path);
        metadataMap.put("language", language);

        Document file = new Document(header + content, metadataMap);

        List<Document> chunks = tokenTextSplitter.apply(List.of(file));

        List<Document> finalChunks=new ArrayList<>();

        for(int i=0;i<chunks.size();i++){
            Document chunk=chunks.get(i);
            Map<String, Object> chunkMetadataMap = new HashMap<>(metadataMap);
            chunkMetadataMap.put("chunkIndex", i);
            int startLine = estimateStartLine(chunks, i, content);
            int endLine = estimateEndLine(startLine, chunk.getText());
            chunkMetadataMap.put("startLine", startLine);
            chunkMetadataMap.put("endLine", endLine);
            finalChunks.add(new Document(chunk.getText() , chunkMetadataMap));
        }
        return finalChunks;
    }

    private int estimateStartLine(
            List<Document> chunks,
            int index,
            String originalContent
    ) {
        int lines = 1;

        for (int i = 0; i < index; i++) {
            lines += countLines(chunks.get(i).getText());
        }

        return lines;
    }

    private int estimateEndLine(int startLine, String chunkText) {
        return startLine + countLines(chunkText) - 1;
    }

    private int countLines(String text) {
        return (int) text.chars()
                .filter(c -> c == '\n')
                .count() + 1;
    }
}

