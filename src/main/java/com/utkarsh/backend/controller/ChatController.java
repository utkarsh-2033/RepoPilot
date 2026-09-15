package com.utkarsh.backend.controller;

import com.utkarsh.backend.dto.ChatRequestDto;
import com.utkarsh.backend.entity.ChatSession;
import com.utkarsh.backend.service.ChatService;
import com.utkarsh.backend.service.auth.AppUserPrincipal;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/session/init/{repoId}")
    public ResponseEntity<ChatSession> initChatSession(@PathVariable UUID repoId,
                                                       @AuthenticationPrincipal AppUserPrincipal user) {
        ChatSession session=chatService.initChatSession(repoId , user.getUser().getId());
        return ResponseEntity.ok().body(session);
    }

    @PostMapping("/session/{sessionId}/message")
    public ResponseEntity<String> sendMessage(@PathVariable UUID sessionId,
                                              @RequestBody ChatRequestDto body,
                                              @AuthenticationPrincipal AppUserPrincipal user) {
        chatService.sendMessage(sessionId, body.question(), user.getUser().getId());
        return ResponseEntity.ok().body("Message sent successfully");
    }

}
