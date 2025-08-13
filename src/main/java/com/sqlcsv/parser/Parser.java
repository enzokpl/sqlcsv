package com.sqlcsv.parser;

import com.sqlcsv.ast.Expr;
import com.sqlcsv.ast.Stmt;
import com.sqlcsv.lexer.Lexer;
import com.sqlcsv.lexer.Token;
import com.sqlcsv.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

import static com.sqlcsv.lexer.TokenType.*;

/**
 * O analisador sintático, responsável por construir uma Árvore de Sintaxe Abstrata (AST) a partir de uma lista de tokens.
 * <p>
 * Utiliza a técnica de "Pratt Parsing" (Top-Down Operator Precedence) para lidar com a precedência de operadores de forma elegante.
 */
public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    /**
     * Constrói um Parser para a lista de tokens fornecida.
     *
     * @param tokens A lista de tokens gerada pelo {@link Lexer}.
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Executa a análise sintática.
     *
     * @return O nó raiz da AST, que é uma {@link Stmt.Select}.
     * @throws ParseError se um erro de sintaxe for encontrado.
     */
    public Stmt.Select parse() {
        try {
            return selectStatement();
        } catch (ParseError error) {
            throw error;
        }
    }

    private Stmt.Select selectStatement() {
        consume(SELECT, "Expect 'SELECT'.");
        List<Stmt.SelectItem> projections = projectionList();

        consume(FROM, "Expect 'FROM' after projections.");
        Token tableName = consume(IDENTIFIER, "Expect table name.");

        Expr whereClause = null;
        if (match(WHERE)) {
            whereClause = expression();
        }

        List<Stmt.OrderItem> orderBy = null;
        if (match(ORDER)) {
            consume(BY, "Expect 'BY' after 'ORDER'.");
            orderBy = orderByList();
        }

        Expr limit = null;
        if (match(LIMIT)) {
            Token limitToken = consume(INTEGER, "Expect an integer for LIMIT clause.");
            limit = new Expr.Literal(limitToken.literal());
        }

        consume(EOF, "Expect end of statement.");
        return new Stmt.Select(projections, tableName, whereClause, orderBy, limit);
    }

    private List<Stmt.SelectItem> projectionList() {
        List<Stmt.SelectItem> items = new ArrayList<>();

        if (check(STAR) && !checkNext(DOT)) {
            advance(); // Consume '*'
            items.add(new Stmt.SelectItem(null, null));
            if (match(COMMA)) {
                throw error(peek(), "Cannot have other columns after '*'.");
            }
            return items;
        }

        do {
            Expr expr = expression();
            String alias = null;
            if (match(AS)) {
                alias = consume(IDENTIFIER, "Expect alias after 'AS'.").lexeme();
            }
            items.add(new Stmt.SelectItem(expr, alias));
        } while (match(COMMA));

        return items;
    }

    private List<Stmt.OrderItem> orderByList() {
        List<Stmt.OrderItem> items = new ArrayList<>();
        do {
            Expr expr = expression();
            boolean isAsc = true;
            if (match(DESC)) {
                isAsc = false;
            } else if (match(ASC)) {
                isAsc = true;
            }
            items.add(new Stmt.OrderItem(expr, isAsc));
        } while (match(COMMA));
        return items;
    }

    private Expr expression() {
        return parsePrecedence(Precedence.ASSIGNMENT);
    }

    private Expr parsePrecedence(Precedence precedence) {
        Token prefixToken = advance();
        ParseRule prefixRule = getRule(prefixToken.type());

        if (prefixRule.prefix == null) {
            throw error(prefixToken, "Expect expression.");
        }

        Expr left = prefixRule.prefix.apply(this);

        while (precedence.ordinal() <= getRule(peek().type()).precedence.ordinal()) {
            Token infixToken = advance();
            ParseRule infixRule = getRule(infixToken.type());
            left = infixRule.infix.apply(this, left);
        }

        return left;
    }

    private enum Precedence {
        NONE, ASSIGNMENT, OR, AND, EQUALITY, COMPARISON, TERM, FACTOR, UNARY, CALL, PRIMARY
    }

    @FunctionalInterface interface PrefixParseFn { Expr apply(Parser p); }
    @FunctionalInterface interface InfixParseFn { Expr apply(Parser p, Expr left); }
    private record ParseRule(PrefixParseFn prefix, InfixParseFn infix, Precedence precedence) {}

    private ParseRule getRule(TokenType type) {
        return switch (type) {
            case LEFT_PAREN -> new ParseRule(Parser::grouping, null, Precedence.NONE);
            case MINUS -> new ParseRule(Parser::unary, Parser::binary, Precedence.TERM);
            case PLUS -> new ParseRule(null, Parser::binary, Precedence.TERM);
            case SLASH, STAR, PERCENT -> new ParseRule(null, Parser::binary, Precedence.FACTOR);
            case EQUAL, BANG_EQUAL, NOT_EQUAL -> new ParseRule(null, Parser::binary, Precedence.EQUALITY);
            case GREATER, GREATER_EQUAL, LESS, LESS_EQUAL -> new ParseRule(null, Parser::binary, Precedence.COMPARISON);
            case AND -> new ParseRule(null, Parser::binary, Precedence.AND);
            case OR -> new ParseRule(null, Parser::binary, Precedence.OR);
            case NOT -> new ParseRule(Parser::unary, null, Precedence.UNARY);
            case IDENTIFIER -> new ParseRule(Parser::identifier, null, Precedence.NONE);
            case INTEGER, DOUBLE, STRING, TRUE, FALSE, NULL -> new ParseRule(Parser::literal, null, Precedence.NONE);
            default -> new ParseRule(null, null, Precedence.NONE);
        };
    }

    private Expr grouping() {
        Expr expr = expression();
        consume(RIGHT_PAREN, "Expect ')' after expression.");
        return new Expr.Grouping(expr);
    }

    private Expr unary() {
        Token operator = previous();
        Expr right = parsePrecedence(Precedence.UNARY);
        return new Expr.Unary(operator, right);
    }

    private Expr binary(Expr left) {
        Token operator = previous();
        ParseRule rule = getRule(operator.type());
        Expr right = parsePrecedence(Precedence.values()[rule.precedence.ordinal() + 1]);
        return new Expr.Binary(left, operator, right);
    }

    private Expr literal() {
        Token token = previous();
        return switch (token.type()) {
            case TRUE -> new Expr.Literal(true);
            case FALSE -> new Expr.Literal(false);
            case NULL -> new Expr.Literal(null);
            default -> new Expr.Literal(token.literal());
        };
    }

    private Expr identifier() {
        return new Expr.Identifier(previous());
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (peek().type() == TokenType.EOF) {
            return type == TokenType.EOF;
        }
        return peek().type() == type;
    }

    private boolean checkNext(TokenType type) {
        if (isAtEnd() || tokens.get(current + 1).type() == EOF) return false;
        return tokens.get(current + 1).type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        return new ParseError(token, message);
    }
}