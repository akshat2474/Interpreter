package com.lox;

import java.util.List;

/**
 * An interface for any object that can be "called"
 * (like a function).
 */
interface LoxCallable {
    /**
     * Returns the number of arguments the callable expects.
     */
    int arity();
    
    /**
     * Executes the callable.
     * @param interpreter The interpreter instance (for context).
     * @param arguments The list of evaluated arguments.
     * @return The return value of the call.
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}
