package com.utkarsh.backend.service;

import com.utkarsh.backend.dto.ChatMessageResponseDto;
import com.utkarsh.backend.dto.CitationDto;
import com.utkarsh.backend.dto.SessionsResponseDto;
import com.utkarsh.backend.entity.ChatMessage;
import com.utkarsh.backend.entity.ChatSession;
import com.utkarsh.backend.entity.Repository;
import com.utkarsh.backend.exception.BadRequestException;
import com.utkarsh.backend.exception.SessionNotFoundException;
import com.utkarsh.backend.repository.ChatMessageRepo;
import com.utkarsh.backend.repository.ChatRepo;
import com.utkarsh.backend.repository.RepositoryRepo;
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
    private final RepositoryRepo repositoryRepo;

    public ChatService(ChatMessageRepo chatMessageRepo,
                       ChatRepo chatRepo,
                       Retrieval retrieval,
                       ChatClient chatClient,
                       PromptBuilder promptBuilder,
                       CitationMapper citationMapper,
                       RepositoryRepo repositoryRepo
    ) {
        this.chatMessageRepo = chatMessageRepo;
        this.chatRepo = chatRepo;
        this.retrieval = retrieval;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.citationMapper=citationMapper;
        this.repositoryRepo=repositoryRepo;
    }

    @Value("${retrieval.top-k:8}")
    private int topK;

    public SessionsResponseDto initChatSession(UUID repoId , UUID userId) {
        ChatSession chatSession = new ChatSession();
        Repository repo= repositoryRepo.findById(repoId).orElseThrow(()-> new BadRequestException("Repository not found"));
        chatSession.setRepositoryId(repoId);
        chatSession.setUserid(userId);
        chatSession.setTitle(repo.getName() + " - " + System.currentTimeMillis());
        chatRepo.save(chatSession);

        SessionsResponseDto responseDto = new SessionsResponseDto(
                chatSession.getId(),
                chatSession.getTitle(),
                chatSession.getUpdatedAt(),
                chatSession.getRepositoryId(),
                repo.getName()
        );
        return responseDto;
    }

    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession chatSession = chatRepo.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Chat session with id " + sessionId + " not found"));
        if (!chatSession.getUserid().equals(userId)) {
            throw new BadRequestException("User not authorized to delete this chat session");
        }
        chatMessageRepo.deleteAllBySessionId(sessionId);
        chatRepo.deleteById(sessionId);
    }

    public ChatStreamResult messageStream(UUID sessionId, String question, UUID id) {

        ChatSession chatSession = chatRepo.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Chat session not found"));
        UUID userId = chatSession.getUserid();
        if (!userId.equals(id)) {
            throw new BadRequestException("User not authorized to send message in this chat session");
        }
        UUID repoId = chatSession.getRepositoryId();

        chatMessageRepo.save(ChatMessage.builder()
                .sessionId(sessionId)
                .message(question)
                .role(ChatMessage.Role.USER)
                .build());

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

    public List<SessionsResponseDto> getAllSessions(UUID id) {

        List<ChatSession> sessions =chatRepo.findByUserid(id);

        return sessions.stream()
                .map(session -> new SessionsResponseDto(
                        session.getId(),
                        session.getTitle(),
                        session.getUpdatedAt(),
                        session.getRepositoryId(),
                        null
                ))
                .toList();
    }

    public List<ChatMessageResponseDto> getSessionMessages(UUID sessionId, UUID userId) {
        ChatSession chatSession = chatRepo.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Chat session with id " + sessionId + " not found"));
        if (!chatSession.getUserid().equals(userId)) {
            throw new BadRequestException("User not authorized to this chat session");
        }
        List<ChatMessage> messages = chatMessageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);

        return messages.stream()
                .map(message -> new ChatMessageResponseDto(
                        message.getId(),
                        message.getMessage(),
                        message.getRole().name(),
                        message.getCreatedAt(),
                        message.getCitations()
                ))
                .toList();
    }

}
