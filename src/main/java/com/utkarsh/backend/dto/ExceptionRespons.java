package com.utkarsh.backend.dto;

import java.time.Instant;

public record ExceptionRespons(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path
) {
}
