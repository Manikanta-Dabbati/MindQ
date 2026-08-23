package com.mindq.mcq.exception;

/**
 * Thrown when the AI response is not valid MCQ JSON
 * or fails validation (wrong structure, missing fields, etc.).
 */
public class InvalidMcqResponseException extends RuntimeException {

    public InvalidMcqResponseException(String message) {
        super(message);
    }

    public InvalidMcqResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
