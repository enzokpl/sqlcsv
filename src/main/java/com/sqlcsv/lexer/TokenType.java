package com.sqlcsv.lexer;

/**
 * Enumeração de todos os tipos de tokens reconhecidos pelo Lexer.
 * <br>
 * Inclui palavras-chave, operadores, literais e caracteres de pontuação.
 */
public enum TokenType {
    // Tokens de um único caractere
    LEFT_PAREN, RIGHT_PAREN, COMMA, DOT, MINUS, PLUS, SLASH, STAR, PERCENT,
    EQUAL, GREATER, LESS,

    // Tokens de um ou dois caracteres
    BANG, BANG_EQUAL,
    EQUAL_EQUAL,
    GREATER_EQUAL,
    LESS_EQUAL,
    NOT_EQUAL, // <>

    // Literais
    IDENTIFIER, STRING, INTEGER, DOUBLE,

    // Palavras-chave
    AND, AS, ASC, BY, DESC, FALSE, FROM, LIMIT, NOT, NULL, OR,
    ORDER, SELECT, TRUE, WHERE,

    EOF
}