package com.utkarsh.backend.repository;

import com.utkarsh.backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRepo extends JpaRepository<ChatSession, UUID> {

//    Optional<ChatSession> findByIdAndUserid(UUID id, UUID userid);

    List<ChatSession> findByUserid(UUID id);
}
