package com.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The main entry point for the Lox interpreter.
 * Handles reading source code from a file or from the interactive REPL.
 */
public class Lox {
    // Static fields to track if an error has occurred.
    // This prevents executing code that has known errors.
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    // The interpreter instance that will execute the code.
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            // Invalid usage
            System.out.println("Usage: jlox [script]");
            System.exit(64); 
        } else if (args.length == 1) {
            // Run from a source file
            runFile(args[0]);
        } else {
            // Run the interactive prompt (REPL)
            runPrompt();
        }
    }

    /**
     * Reads a source file and executes it.
     * @param path The path to the source file.
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * Starts the interactive Read-Evaluate-Print-Loop (REPL).
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) { 
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break; // User pressed Ctrl+D
            run(line);
            // In REPL mode, we reset the error flag so the user can continue.
            hadError = false;
        }
    }

    /**
     * The core interpreter pipeline.
     * Scans, parses, and interprets the source code.
     * @param source The source code as a string.
     */
    private static void run(String source) {
        // 1. Scanner (Lexer): String -> List<Token>
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // 2. Parser: List<Token> -> List<Stmt> (AST)
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax (parse) error.
        if (hadError) return;

        // 3. Interpreter: List<Stmt> -> Execution
        interpreter.interpret(statements);
    }

    // --- Error Reporting ---

    /**
     * Reports a syntax error.
     * @param line The line number where the error occurred.
     * @param message The error message.
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Reports a runtime error.
     * @param error The RuntimeError exception.
     */
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    /**
     * Helper to report an error at a specific token.
     * @param token The token where the error occurred.
     * @param message The error message.
     */
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    /**
     * General-purpose error reporting utility.
     * @param line The line number.
     * @param where A description of the location (e.g., "at 'x'").
     * @param message The error message.
     */
    private static void report(int line, String where, String message) {
        System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
