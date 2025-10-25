package com.lox;

/**
 * A data class representing a single Token.
 * It stores the token's type, its original string (lexeme),
 * its literal value (if any), and the line number it appeared on.
 */
class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line; 

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + (literal != null ? literal : "");
    }
}

