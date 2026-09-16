package com.utkarsh.backend.controller;

import com.utkarsh.backend.dto.ChatMessageResponseDto;
import com.utkarsh.backend.dto.ChatRequestDto;
import com.utkarsh.backend.dto.SessionsResponseDto;
import com.utkarsh.backend.service.ChatService;
import com.utkarsh.backend.service.auth.AppUserPrincipal;
import com.utkarsh.backend.service.chat.ChatStreamHandler;
import com.utkarsh.backend.service.chat.ChatStreamResult;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatStreamHandler chatStreamHandler;


    @PostMapping("/session/init/{repoId}")
    public ResponseEntity<SessionsResponseDto> initChatSession(@PathVariable UUID repoId,
                                                       @AuthenticationPrincipal AppUserPrincipal user) {
        SessionsResponseDto session=chatService.initChatSession(repoId , user.getUser().getId());
        return ResponseEntity.ok().body(session);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionsResponseDto>> getAllSessions(@AuthenticationPrincipal AppUserPrincipal user) {
        return ResponseEntity.ok().body(chatService.getAllSessions(user.getUser().getId()));
    }

    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getAllMessages(@PathVariable UUID sessionId,
                                                                       @AuthenticationPrincipal AppUserPrincipal user) {
        return ResponseEntity.ok().body(chatService.getSessionMessages(sessionId, user.getUser().getId()));
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId,
                                              @AuthenticationPrincipal AppUserPrincipal user) {
        chatService.deleteSession(sessionId, user.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/session/{sessionId}/message",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter messageStream(
            @PathVariable UUID sessionId,
            @RequestBody ChatRequestDto body,
            @AuthenticationPrincipal AppUserPrincipal user) {

        ChatStreamResult result = chatService.messageStream( sessionId,  body.question(), user.getUser().getId());

        return chatStreamHandler.handle(result , sessionId);
    }
}
