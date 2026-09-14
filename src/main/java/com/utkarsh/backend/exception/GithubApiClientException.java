package com.utkarsh.backend.exception;

import org.springframework.http.HttpStatus;

public class GithubApiClientException extends RuntimeException {
    private final HttpStatus statusCode;
    public GithubApiClientException(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
