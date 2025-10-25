package com.lox;

import java.util.List;

/**
 * The base class for all Expression AST (Abstract Syntax Tree) nodes.
 * This uses the Visitor pattern to allow the Interpreter
 * to visit each node type.
 */
abstract class Expr {
  /**
   * The Visitor interface defines a visit method for each expression type.
   */
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
  }

  /**
   * Each expression node class must implement this method.
   */
  abstract <R> R accept(Visitor<R> visitor);

  // --- Nested Classes for each Expression Type ---

  /**
   * An assignment expression, e.g., `a = 10`.
   */
  static class Assign extends Expr {
    final Token name;
    final Expr value;

    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }
  }

  /**
   * A binary operation, e.g., `a + b`.
   */
  static class Binary extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }
  }

  /**
   * A function call, e.g., `myFunction(arg1)`.
   */
  static class Call extends Expr {
    final Expr callee;
    final Token paren; // The closing parenthesis ')' for error reporting
    final List<Expr> arguments;

    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }
  }

  /**
   * A grouping, e.g., `(a + b)`.
   */
  static class Grouping extends Expr {
    final Expr expression;

    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }
  }

  /**
   * A literal value, e.g., `123`, `"hello"`, `true`.
   */
  static class Literal extends Expr {
    final Object value;

    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }
  }

  /**
   * A logical operation, e.g., `a and b`.
   */
  static class Logical extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }
  }

  /**
   * A unary operation, e.g., `!a`, `-b`.
   */
  static class Unary extends Expr {
    final Token operator;
    final Expr right;

    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }
  }

  /**
   * A variable, e.g., `myVariable`.
   */
  static class Variable extends Expr {
    final Token name;

    Variable(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }
  }
}
