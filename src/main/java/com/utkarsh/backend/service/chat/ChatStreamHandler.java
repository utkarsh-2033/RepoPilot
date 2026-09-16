package com.utkarsh.backend.service.chat;

import com.utkarsh.backend.dto.CitationDto;
import com.utkarsh.backend.entity.ChatMessage;
import com.utkarsh.backend.repository.ChatMessageRepo;
import com.utkarsh.backend.service.citation.CitationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStreamHandler {

    private final CitationValidator citationValidator;
    private final ChatMessageRepo chatMessageRepo;

    public SseEmitter handle(ChatStreamResult result , UUID sessionId) {

        SseEmitter emitter = new SseEmitter(300_000L);
        StringBuilder fullResponse = new StringBuilder();
        result.tokens().subscribe(

                token -> {
                    fullResponse.append(token);

                    try {
                        emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(token)
                        );
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },

                // onError
                emitter::completeWithError,

                // onComplete
                () -> {

                    try {
                        String answer = fullResponse.toString();

                        List<CitationDto> validCitations =
                                citationValidator.validate(
                                        answer,
                                        result.availableCitations()
                                );
//                        log.info("Valid citations: {}", validCitations);
                        ChatMessage assistantMessage =
                                ChatMessage.builder()
                                        .sessionId(sessionId)
                                        .message(answer)
                                        .role(ChatMessage.Role.ASSISTANT)
                                        .citations(validCitations)
                                        .build();

                        chatMessageRepo.save(assistantMessage);

                        emitter.send(
                                SseEmitter.event()
                                        .name("citations")
                                        .data(validCitations)
                        );

                        emitter.send(
                                SseEmitter.event()
                                        .name("done")
                                        .data("[DONE]")
                        );

                        emitter.complete();

                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }

                }
        );

        return emitter;
    }
}