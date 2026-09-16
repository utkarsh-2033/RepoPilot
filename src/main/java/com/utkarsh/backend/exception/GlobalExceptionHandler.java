package com.utkarsh.backend.exception;

import com.utkarsh.backend.dto.ExceptionRespons;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RepositoryNotFoundException.class)
    public ResponseEntity<ExceptionRespons> handleRepositoryNotFound(RepositoryNotFoundException e, HttpServletRequest req) {
        ExceptionRespons exceptionResponse = new ExceptionRespons(
                Instant.now(),
                404,
                "Not Found",
                e.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(404).body(exceptionResponse);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ExceptionRespons> handleUserNotFound(SessionNotFoundException e, HttpServletRequest req) {
        ExceptionRespons exceptionResponse = new ExceptionRespons(
                Instant.now(),
                404,
                "Not Found",
                e.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(404).body(exceptionResponse);
    }

    @ExceptionHandler(GithubApiClientException.class)
    public ResponseEntity<ExceptionRespons> handlerGithubApiClientException(GithubApiClientException e, HttpServletRequest req) {
        ExceptionRespons exceptionResponse = new ExceptionRespons(
                Instant.now(),
                e.getStatusCode().value(),
                "GITHUB_API_ERROR",
                e.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(e.getStatusCode().value()).body(exceptionResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionRespons> handleRuntimeException(RuntimeException e, HttpServletRequest req) {
        ExceptionRespons exceptionResponse = new ExceptionRespons(
                Instant.now(),
                500,
                "",
                e.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(500).body(exceptionResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionRespons> handleException(Exception e , HttpServletRequest req) {
        ExceptionRespons exceptionResponse = new ExceptionRespons(
               Instant.now(),
                500,
                "Internal Server Error",
                "An unexpected error occurred",
                req.getRequestURI()
        );
        return ResponseEntity.status(500).body(exceptionResponse);
    }
}
