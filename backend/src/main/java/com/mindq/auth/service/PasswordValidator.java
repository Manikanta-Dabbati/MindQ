package com.mindq.auth.service;

import com.mindq.auth.exception.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Enforces strong password policies.
 * Requirements: min 8 chars, uppercase, lowercase, digit, special char.
 */
@Component
public class PasswordValidator {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`]");

    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters");
        }
        if (password.length() > 64) {
            throw new InvalidPasswordException("Password must be at most 64 characters");
        }
        if (!UPPERCASE.matcher(password).find()) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE.matcher(password).find()) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new InvalidPasswordException("Password must contain at least one digit");
        }
        if (!SPECIAL.matcher(password).find()) {
            throw new InvalidPasswordException("Password must contain at least one special character (!@#$%^&* etc.)");
        }
    }
}
