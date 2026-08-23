package com.mindq.ai.exception;

import lombok.Getter;

/**
 * Thrown when an AI provider call fails.
 * Carries an HTTP status code so the GlobalExceptionHandler can map it correctly.
 */
@Getter
public class AiProviderException extends RuntimeException {

    private final int httpStatus;

    public AiProviderException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public AiProviderException(String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}
