package com.lox;

import java.util.List;

/**
 * The runtime representation of a user-defined function.
 */
class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure; // The environment where the function was *defined*

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // 1. Create a new environment for the function's *execution*
        //    This environment is nested inside the function's *closure*.
        Environment environment = new Environment(closure);

        // 2. Bind the arguments to the parameters in this new environment
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme,
                arguments.get(i));
        }

        // 3. Execute the function body in this new environment
        //    We use a try-catch to "catch" the 'return' exception.
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            // This is the normal 'return' path
            return returnValue.value;
        }

        // If the function completes without a 'return', it implicitly returns 'nil'.
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
