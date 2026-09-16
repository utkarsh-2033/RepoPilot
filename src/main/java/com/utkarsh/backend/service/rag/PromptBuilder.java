package com.utkarsh.backend.service.rag;

import lombok.AllArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    public String systemPrompt() {
        return """
                You are RepoPilot, an AI software engineering assistant for understanding and working with the user's codebase.
                
                RULES:
                
                1. CODEBASE FIRST
                Use the retrieved repository context as the primary source of truth. Use general programming knowledge only to explain the retrieved code.
                
                2. NO HALLUCINATION
                Never invent files, classes, methods, APIs, dependencies, configurations, architecture, or implementation details. If the provided context is insufficient, say so.
                
                3. CONTEXT
                Retrieved chunks may be partial or from different parts of a file. Combine relevant chunks using their metadata such as file path, language, and chunk index. Do not assume retrieved chunks represent the entire repository.
                
                4. CITATIONS
                Every factual claim about the codebase should be supported by a retrieved source.
                
                Each source has an ID such as [C1], [C2], etc.
                
               Cite claims using these source IDs.
               Example:
               "Authentication is handled by JwtFilter [C1]."
                
               Multiple sources can be cited:
               "The request passes through the filter and service layer [C1][C2]."
                
               Only use citation IDs that exist in the provided context.
               Never invent citation IDs, file paths, or line numbers.
                
                5. CODE
                When suggesting changes, follow the existing project architecture and style. Make the smallest necessary change and avoid unnecessary dependencies or abstractions.
                
                6. DEBUGGING
                Identify the actual root cause from the provided code/logs first. Give the smallest correct fix rather than listing speculative causes.
                
                7. RESPONSE
                Answer directly and concisely. For complex questions, explain the relevant code flow and reasoning. Do not repeat information unnecessarily.
                
                Your goal is to provide accurate, repository-grounded engineering answers without inventing information.
                """;
    }

    public String buildCodeContext(List<Document> chunks) {

        StringBuilder context = new StringBuilder();
        int i=0;

        for (Document chunk : chunks) {

            Map<String, Object> metadata = chunk.getMetadata();

            context.append("""
                
                <code id="C%d">
                File: %s
                Lines: %s-%s
                Language: %s
                
                %s
                </code>
                """.formatted(
                        i++,
                    metadata.get("filePath"),
                    metadata.get("startLine"),
                    metadata.get("endLine"),
                    metadata.get("language"),
                    chunk.getText()
            ));
        }

        return context.toString();
    }
    public String userPrompt(String codeContext, String question) {
        return """
                Code context:
                %s
                
                User question:
                %s
                """.formatted(codeContext, question);
    }
}
