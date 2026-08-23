package com.mindq.mcq.exception;

/**
 * Thrown when the MCQ generation pipeline fails
 * (AI provider error, parsing error, DB error, etc.).
 * The GlobalExceptionHandler maps this to 502 Bad Gateway.
 */
public class McqGenerationException extends RuntimeException {

    public McqGenerationException(String message) {
        super(message);
    }

    public McqGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
