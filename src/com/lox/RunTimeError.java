package com.lox;

/**
 * A custom exception to represent an error that
 * occurs at runtime (during interpretation).
 */
class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
