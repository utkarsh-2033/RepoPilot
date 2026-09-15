package com.utkarsh.backend.service.indexing;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FileChunking {

    @Autowired
    private TokenTextSplitter tokenTextSplitter;
    private FileFilter fileFilter;

    public List<Document> chunkFile(String path , String content, Long repoId){
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
            finalChunks.add(new Document(chunk.getText() , chunkMetadataMap));
        }
        return finalChunks;
    }
}
