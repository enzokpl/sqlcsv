package com.sqlcsv.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O analisador léxico (scanner), responsável por converter uma string de código SQL em uma lista de {@link Token}s.
 */
public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("AND",    TokenType.AND);
        keywords.put("AS",     TokenType.AS);
        keywords.put("ASC",    TokenType.ASC);
        keywords.put("BY",     TokenType.BY);
        keywords.put("DESC",   TokenType.DESC);
        keywords.put("FALSE",  TokenType.FALSE);
        keywords.put("FROM",   TokenType.FROM);
        keywords.put("LIMIT",  TokenType.LIMIT);
        keywords.put("NOT",    TokenType.NOT);
        keywords.put("NULL",   TokenType.NULL);
        keywords.put("OR",     TokenType.OR);
        keywords.put("ORDER",  TokenType.ORDER);
        keywords.put("SELECT", TokenType.SELECT);
        keywords.put("TRUE",   TokenType.TRUE);
        keywords.put("WHERE",  TokenType.WHERE);
    }

    /**
     * Constrói um Lexer para a string de código SQL fornecida.
     *
     * @param source O código SQL a ser tokenizado.
     */
    public Lexer(String source) {
        this.source = source;
    }

    /**
     * Executa a análise léxica e retorna a lista de tokens.
     * <br>
     * A lista sempre terminará com um token do tipo {@link TokenType#EOF}.
     *
     * @return Uma lista de {@link Token}s.
     * @throws LexerError se um caractere inesperado for encontrado.
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line, col));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENT); break;
            case '/': addToken(TokenType.SLASH); break;

            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(TokenType.EQUAL);
                break;
            case '<':
                addToken(match('>') ? TokenType.NOT_EQUAL : (match('=') ? TokenType.LESS_EQUAL : TokenType.LESS));
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            case '\n':
                line++;
                col = 1;
                break;

            case '\'': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new LexerError("Unexpected character.", line, col);
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text.toUpperCase());
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the "."
            while (isDigit(peek())) advance();
            addToken(TokenType.DOUBLE, Double.parseDouble(source.substring(start, current)));
        } else {
            addToken(TokenType.INTEGER, Integer.parseInt(source.substring(start, current)));
        }
    }

    private void string() {
        while (peek() != '\'' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                col = 0; // Will be incremented by advance
            }
            advance();
        }

        if (isAtEnd()) {
            throw new LexerError("Unterminated string.", line, col);
        }

        advance(); // The closing '.

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        col++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        col++;
        return c;
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, start + 1));
    }

    /**
     * Uma exceção que representa um erro durante a análise léxica.
     */
    public static class LexerError extends RuntimeException {
        public LexerError(String message, int line, int col) {
            super(String.format("[line %d:%d] Error: %s", line, col, message));
        }
    }
}