package com.sqlcsv.ast;

import com.sqlcsv.lexer.Token;

/**
 * Representa a interface selada para todas as nós de expressão na Árvore de Sintaxe Abstrata (AST).
 * <br>
 * Utiliza o padrão {@link Visitor} para avaliação e outras operações sobre a AST.
 */
public sealed interface Expr {

    /**
     * Aceita um visitor para processar esta expressão.
     *
     * @param visitor O {@link Visitor} que irá operar sobre este nó.
     * @param <R> O tipo de retorno do método do visitor.
     * @return O resultado da operação do visitor.
     */
    <R> R accept(Visitor<R> visitor);

    /**
     * Interface do {@link Visitor} para as expressões.
     *
     * @param <R> O tipo de retorno dos métodos de visita.
     */
    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitUnaryExpr(Unary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitIdentifierExpr(Identifier expr);
    }

    /**
     * Representa uma expressão binária, como 'a + b'.
     *
     * @param left A expressão à esquerda do operador.
     * @param operator O token do operador.
     * @param right A expressão à direita do operador.
     */
    record Binary(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    /**
     * Representa uma expressão unária, como '-a' ou 'NOT b'.
     *
     * @param operator O token do operador unário.
     * @param right A expressão à direita do operador.
     */
    record Unary(Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    /**
     * Representa uma expressão entre parênteses para agrupamento, como '(a + b)'.
     *
     * @param expression A expressão contida dentro dos parênteses.
     */
    record Grouping(Expr expression) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    /**
     * Representa um valor literal, como um número, string, booleano ou nulo.
     *
     * @param value O valor literal Java correspondente.
     */
    record Literal(Object value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    /**
     * Representa um identificador, geralmente um nome de coluna.
     *
     * @param name O token do identificador.
     */
    record Identifier(Token name) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIdentifierExpr(this);
        }
    }
}