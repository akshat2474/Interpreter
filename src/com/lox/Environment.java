package com.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the state (variables) of the program.
 * Environments are chained together to create lexical scopes.
 */
class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Constructor for the global environment.
     */
    Environment() {
        enclosing = null;
    }

    /**
     * Constructor for nested (local) scopes.
     * @param enclosing The outer scope.
     */
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Defines a new variable in the *current* scope.
     */
    void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Gets the value of a variable.
     * If not found in this scope, it checks the enclosing (outer) scope.
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // Recursively check the parent environment
        if (enclosing != null) return enclosing.get(name);

        // If not found anywhere, it's a runtime error.
        throw new RuntimeError(name,
            "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * Assigns a new value to an *existing* variable.
     * If not found in this scope, it checks the enclosing scope.
     */
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // Recursively check the parent environment
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        // If not found anywhere, it's a runtime error.
        throw new RuntimeError(name,
            "Undefined variable '" + name.lexeme + "'.");
    }
}
