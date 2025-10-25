package com.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * The Interpreter walks the AST (Expr and Stmt nodes)
 * and executes the program.
 *
 * It implements the Visitor pattern for both expressions and statements.
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // The 'globals' environment stores native functions (like 'clock').
    final Environment globals = new Environment();
    // 'environment' tracks the *current* scope.
    private Environment environment = globals;
    // 'locals' is used for variable resolution (not fully implemented here
    // but essential for a more advanced interpreter).

    Interpreter() {
        // Define a native 'clock' function
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    /**
     * Main entry point. Interprets a list of statements.
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /**
     * Executes a single statement.
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Executes a block of statements in a new, nested scope.
     * @param statements The statements in the block.
     * @param environment The new environment for this block.
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // Restore the previous environment when the block is exited
            // (even if an exception occurs).
            this.environment = previous;
        }
    }

    /**
     * Evaluates a single expression.
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // --- Statement Visitor Implementations ---

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // Create a new environment nested within the current one.
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression); // Evaluate for side effects
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // Create a function object and define it in the *current* environment.
        // The function 'closes over' the current environment.
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        // We use an exception to unwind the call stack to the call site.
        throw new Return(value);
    }

    @Override
    public Void visitLetStmt(Stmt.Let stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        // Define the variable in the current environment.
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    // --- Expression Visitor Implementations ---

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        // Handle short-circuiting
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else { // AND
            if (!isTruthy(left)) return left;
        }
        
        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            default:
                // Unreachable.
                return null;
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right); 

        switch (expr.operator.type) {
            // Arithmetic
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                // '+' is overloaded for numbers and strings
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                } 
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                // Allow string concatenation with numbers
                if (left instanceof String && right instanceof Double) {
                    return (String)left + stringify(right);
                }
                if (left instanceof Double && right instanceof String) {
                    return stringify(left) + (String)right;
                }
                throw new RuntimeError(expr.operator, 
                    "Operands must be two numbers or two strings.");

            // Comparison
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;

            // Equality
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            
            default:
                // Unreachable.
                return null;
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        // Evaluate all arguments first
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) { 
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;

        // Check arity (number of arguments)
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                function.arity() + " arguments but got " +
                arguments.size() + ".");
        }

        try {
            return function.call(this, arguments);
        } catch (Return returnValue) {
            // This is how we handle 'return'. We catch the exception
            // and return its value.
            return returnValue.value;
        }
    }

    // --- Interpreter Helper Methods ---

    /**
     * Lox follows Ruby's rule: 'false' and 'nil' are falsey,
     * everything else is truthy.
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    /**
     * Checks for Lox-style equality.
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    /**
     * Converts a Lox object to a Java string for printing.
     */
    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    /**
     * Runtime check that an operand is a number.
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    /**
     * Runtime check that both operands are numbers.
     */
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
}
