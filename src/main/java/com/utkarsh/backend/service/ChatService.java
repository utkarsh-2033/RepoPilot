package com.utkarsh.backend.service;

import com.utkarsh.backend.dto.CitationDto;
import com.utkarsh.backend.entity.ChatSession;
import com.utkarsh.backend.exception.BadRequestException;
import com.utkarsh.backend.repository.ChatMessageRepo;
import com.utkarsh.backend.repository.ChatRepo;
import com.utkarsh.backend.service.chat.ChatStreamResult;
import com.utkarsh.backend.service.citation.CitationMapper;
import com.utkarsh.backend.service.rag.PromptBuilder;
import com.utkarsh.backend.service.rag.Retrieval;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private final ChatMessageRepo chatMessageRepo;
    private final ChatRepo chatRepo;
    private final Retrieval retrieval;
    private final ChatClient chatClient;
    private final PromptBuilder promptBuilder;
    private final CitationMapper citationMapper;

    public ChatService(ChatMessageRepo chatMessageRepo,
                       ChatRepo chatRepo,
                       Retrieval retrieval,
                       ChatClient chatClient,
                       PromptBuilder promptBuilder,
                       CitationMapper citationMapper
    ) {
        this.chatMessageRepo = chatMessageRepo;
        this.chatRepo = chatRepo;
        this.retrieval = retrieval;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.citationMapper=citationMapper;
    }

    @Value("${retrieval.top-k:8}")
    private int topK;

    public ChatSession initChatSession(UUID repoId , UUID userId) {
        ChatSession chatSession = new ChatSession();
        chatSession.setRepositoryId(repoId);
        chatSession.setUserid(userId);
        return chatRepo.save(chatSession);
    }

    public ChatStreamResult sendMessage(UUID sessionId, String question, UUID id) {

        ChatSession chatSession = chatRepo.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Chat session not found"));
        UUID userId = chatSession.getUserid();
        if (!userId.equals(id)) {
            throw new BadRequestException("User not authorized to send message in this chat session");
        }
        UUID repoId = chatSession.getRepositoryId();

        List<Document> topSimilarChunks= retrieval.getTopKChunks(question, repoId, topK);

        List<CitationDto> citations =
                citationMapper.fromDocuments(topSimilarChunks);

        String codeContext=promptBuilder.buildCodeContext(topSimilarChunks);

        Flux<String> tokens = chatClient
                .prompt()
                .system(promptBuilder.systemPrompt())
                .user(promptBuilder.userPrompt(codeContext, question))
                .stream()
                .content();

        return new ChatStreamResult(
                tokens,
                citations
        );

    }
}
