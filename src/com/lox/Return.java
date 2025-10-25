package com.lox;

/**
 * A special exception used to unwind the stack when a
 * 'return' statement is encountered.
 *
 * It's a "trick" to avoid passing a "did we return?"
 * flag all the way up the visitor call stack.
 */
class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        // We disable stack trace generation for performance,
        // as this is used for control flow, not as a true error.
        super(null, null, false, false);
        this.value = value;
    }
}
