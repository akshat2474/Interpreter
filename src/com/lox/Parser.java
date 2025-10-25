package com.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Parser takes the list of Tokens from the Scanner
 * and builds an Abstract Syntax Tree (AST) representing
 * the program's structure.
 *
 * This is a recursive descent parser.
 */
class Parser {
    /**
     * A simple error class used to unwind the parser on a syntax error.
     */
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Main entry point. Parses a list of statements.
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // --- Grammar Rules ---

    /**
     * declaration -> functionDeclaration | letDeclaration | statement
     */
    private Stmt declaration() {
        try {
            if (match(TokenType.FUNCTION)) return function("function");
            if (match(TokenType.LET)) return letDeclaration();
            return statement();
        } catch (ParseError error) {
            // On a syntax error, synchronize to the next statement
            synchronize();
            return null; // Will be filtered out
        }
    }

    /**
     * functionDeclaration -> "function" IDENTIFIER "(" parameters? ")" block
     */
    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    /**
     * letDeclaration -> "let" IDENTIFIER ( "=" expression )? ";"
     */
    private Stmt letDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Let(name, initializer);
    }

    /**
     * statement -> printStatement | ifStatement | whileStatement | forStatement | returnStatement | block | expressionStatement
     */
    private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        
        return expressionStatement();
    }

    /**
     * returnStatement -> "return" expression? ";"
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * forStatement -> "for" "(" ( letDeclaration | expressionStatement | ";" )
     * expression? ";"
     * expression? ")" statement
     *
     * Desugaring: This is syntactic sugar. We transform the 'for' loop
     * into a 'while' loop.
     *
     * for (let i = 0; i < 10; i = i + 1) print i;
     *
     * ...is transformed into...
     *
     * {
     * let i = 0;
     * while (i < 10) {
     * print i;
     * i = i + 1;
     * }
     * }
     */
    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        // 1. Initializer
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.LET)) {
            initializer = letDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // 2. Condition
        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        // 3. Increment
        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

        // 4. Body
        Stmt body = statement();

        // --- Desugaring ---
        
        // If there's an increment, add it to the end of the main body.
        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        // If there's no condition, default to 'true' for an infinite loop.
        if (condition == null) {
            condition = new Expr.Literal(true);
        }
        // Create the 'while' loop with the condition and new body.
        body = new Stmt.While(condition, body);

        // If there's an initializer, run it once before the 'while' loop.
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * whileStatement -> "while" "(" expression ")" statement
     */
    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    /**
     * ifStatement -> "if" "(" expression ")" statement ( "else" statement )?
     */
    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }
        
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * block -> "{" declaration* "}"
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * printStatement -> "print" expression ";"
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * expressionStatement -> expression ";"
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // --- Expression Parsing (by precedence) ---

    /**
     * expression -> assignment
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * assignment -> IDENTIFIER "=" assignment | logic_or
     */
    private Expr assignment() {
        Expr expr = logicOr(); // Parse the left-hand side

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment(); // Recurse to parse the right-hand side

            if (expr instanceof Expr.Variable) {
                // If the left side is a variable, create an assignment node
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            
            // If it's not a valid assignment target, report an error
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * logic_or -> logic_and ( "or" logic_and )*
     */
    private Expr logicOr() {
        Expr expr = logicAnd();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = logicAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * logic_and -> equality ( "and" equality )*
     */
    private Expr logicAnd() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * equality -> comparison ( ( "!=" | "==" ) comparison )*
     */
    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )*
     */
    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * term -> factor ( ( "-" | "+" ) factor )*
     */
    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * factor -> unary ( ( "/" | "*" ) unary )*
     */
    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * unary -> ( "!" | "-" ) unary | call
     */
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * call -> primary ( "(" arguments? ")" )*
     */
    private Expr call() {
        Expr expr = primary();

        // This loop allows for multiple function calls like `getFunc()(arg)`
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Helper for parsing the arguments of a function call.
     */
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    // We report the error but don't throw, to continue parsing
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * primary -> NUMBER | STRING | "true" | "false" | "nil" | IDENTIFIER | "(" expression ")"
     */
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // If no primary expression matches, it's an error.
        throw error(peek(), "Expect expression.");
    }

    // --- Parser Helper Methods ---

    /**
     * Checks if the current token matches any of the given types.
     * If yes, consumes it and returns true.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes the current token if it's of the expected type.
     * If not, throws a ParseError.
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /**
     * Checks if the current token is of the given type (without consuming).
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Consumes and returns the current token.
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Checks if we are at the end of the token list.
     */
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    /**
     * Returns the current token (without consuming).
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the previous token.
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Creates and returns a ParseError.
     */
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Error recovery. Discards tokens until it finds the
     * beginning of the next statement.
     */
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUNCTION:
                case LET:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    // Do nothing, continue discarding
            }
            advance();
        }
    }
}
