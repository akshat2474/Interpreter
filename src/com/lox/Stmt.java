package com.lox;

import java.util.List;

/**
 * The base class for all Statement AST (Abstract Syntax Tree) nodes.
 * This uses the Visitor pattern, just like Expr.
 */
abstract class Stmt {
  /**
   * The Visitor interface defines a visit method for each statement type.
   */
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitLetStmt(Let stmt);
    R visitWhileStmt(While stmt);
  }

  /**
   * Each statement node class must implement this method.
   */
  abstract <R> R accept(Visitor<R> visitor);

  // --- Nested Classes for each Statement Type ---

  /**
   * A block of statements, e.g., `{ ... }`.
   */
  static class Block extends Stmt {
    final List<Stmt> statements;

    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }

  /**
   * A statement that is just a single expression, e.g., `a + b;`.
   */
  static class Expression extends Stmt {
    final Expr expression;

    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }

  /**
   * A function declaration, e.g., `function myFunc() { ... }`.
   */
  static class Function extends Stmt {
    final Token name;
    final List<Token> params;
    final List<Stmt> body;

    Function(Token name, List<Token> params, List<Stmt> body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }
  }

  /**
   * An if-else statement, e.g., `if (cond) { ... } else { ... }`.
   */
  static class If extends Stmt {
    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch; // Can be null

    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }
  }

  /**
   * A print statement, e.g., `print "Hello";`.
   */
  static class Print extends Stmt {
    final Expr expression;

    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
  }

  /**
   * A return statement, e.g., `return 123;`.
   */
  static class Return extends Stmt {
    final Token keyword;
    final Expr value; // Can be null

    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }
  }

  /**
   * A variable declaration, e.g., `let a = 10;`.
   */
  static class Let extends Stmt {
    final Token name;
    final Expr initializer; // Can be null

    Let(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLetStmt(this);
    }
  }

  /**
   * A while loop, e.g., `while (cond) { ... }`.
   */
  static class While extends Stmt {
    final Expr condition;
    final Stmt body;

    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }
  }
}
