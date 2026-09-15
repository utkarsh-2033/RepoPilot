package com.utkarsh.backend.repository;

import com.utkarsh.backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRepo extends JpaRepository<ChatSession, UUID> {
}
