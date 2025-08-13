package com.sqlcsv.parser;

import com.sqlcsv.lexer.Token;

/**
 * Uma exceção que representa um erro sintático encontrado pelo {@link Parser}.
 */
public class ParseError extends RuntimeException {
    public final Token token;

    /**
     * Constrói um erro de parse.
     *
     * @param token O token que causou o erro.
     * @param message A mensagem de erro.
     */
    public ParseError(Token token, String message) {
        super(message);
        this.token = token;
    }
}