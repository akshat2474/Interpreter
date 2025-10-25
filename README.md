# Lox Interpreter

A tree-walk interpreter for the Lox programming language, implemented in Java. This project follows Robert Nystrom's [Crafting Interpreters](https://craftinginterpreters.com/).

## What is Lox?

Lox is a dynamically-typed scripting language with C-style syntax. This interpreter parses Lox source code into an Abstract Syntax Tree (AST) and executes it by traversing the tree structure.

## Features

- **Variables** with lexical scoping
- **Data types**: numbers, strings, booleans, and nil
- **Arithmetic operators**: `+`, `-`, `*`, `/`
- **Comparison operators**: `==`, `!=`, `<`, `<=`, `>`, `>=`
- **Logical operators**: `and`, `or`, `!`
- **Control flow**: `if`/`else`, `while`, `for`
- **Functions**: first-class functions with closures
- **Recursion** support
- **Built-in functions**: `clock()` for timing
- **Interactive REPL** for live coding

## Quick Start

### Compile

```
javac -d . src/com/lox/*.java
```

### Run a Script

```
java com.lox.Lox examples/factorial.lang
```

### Start the REPL

```
java com.lox.Lox
```

## Language Examples

### Variables

```
let name = "Lox";
let version = 1.0;
```

### Functions

```
function greet(name) {
    return "Hello, " + name + "!";
}

print greet("World"); // Hello, World!
```

### Factorial (Recursion)

```
function factorial(n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}

print factorial(5); // 120
```

### Loops

```
// For loop
for (let i = 0; i < 5; i = i + 1) {
    print i;
}

// While loop
let count = 0;
while (count < 5) {
    print count;
    count = count + 1;
}
```

### Closures

```
function makeCounter() {
    let count = 0;
    function increment() {
        count = count + 1;
        return count;
    }
    return increment;
}

let counter = makeCounter();
print counter(); // 1
print counter(); // 2
```

## Project Structure

```
.
├── src/com/lox/
│   ├── Lox.java          # Entry point and REPL
│   ├── Scanner.java      # Lexical analysis
│   ├── Token.java        # Token representation
│   ├── Parser.java       # Syntax analysis
│   ├── Expr.java         # Expression AST nodes
│   ├── Stmt.java         # Statement AST nodes
│   ├── Interpreter.java  # AST evaluator
│   └── Environment.java  # Variable scoping
├── examples/             # Sample Lox programs
└── tests/                # Test suite
```

## How It Works

1. **Scanning**: Source code → tokens
2. **Parsing**: Tokens → Abstract Syntax Tree
3. **Interpreting**: AST traversal and execution

## Testing

Run the test suite against your implementation:

```
# Add your test runner here
java com.lox.Lox tests/test_script.lang
```

## Resources

- [Crafting Interpreters Book](https://craftinginterpreters.com/) - Full implementation guide
- [Official Repository](https://github.com/munificent/craftinginterpreters) - Reference implementations

