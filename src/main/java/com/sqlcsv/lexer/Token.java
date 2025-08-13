package com.sqlcsv.lexer;

public record Token(TokenType type, String lexeme, Object literal, int line, int col) {
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}