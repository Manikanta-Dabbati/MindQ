package com.mindq.enums;

/**
 * Quiz delivery mode.
 * PRACTICE: no timer, free navigation
 * TIMED: countdown timer, free navigation
 * EXAM: countdown timer, auto-submit on timeout, strict submission
 */
public enum QuizMode {
    PRACTICE,
    TIMED,
    EXAM
}
