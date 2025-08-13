package com.sqlcsv.semantic;

/**
 * Uma exceção que representa um erro semântico, como uma coluna não encontrada ou um tipo incompatível.
 */
public class SemanticError extends RuntimeException {

    /**
     * Constrói um novo erro semântico com uma mensagem.
     *
     * @param message a mensagem de erro.
     */
    public SemanticError(String message) {
        super(message);
    }
}