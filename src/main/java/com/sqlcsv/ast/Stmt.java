package com.sqlcsv.ast;

import com.sqlcsv.lexer.Token;
import java.util.List;

/**
 * Representa a interface selada para todas as declarações (statements) na AST.
 * <p>
 * No momento, a única declaração suportada é {@link Select}.
 */
public sealed interface Stmt {

    /**
     * Representa um item na lista de projeção da cláusula SELECT.
     *
     * @param expression A expressão a ser projetada (ex: 'preco * 1.1').
     * @param alias O nome do alias para a expressão (ex: 'total'), ou nulo se não houver.
     */
    record SelectItem(Expr expression, String alias) {}

    /**
     * Representa um termo na cláusula ORDER BY.
     *
     * @param expression A expressão pela qual ordenar.
     * @param isAsc Verdadeiro se a ordenação for ascendente (ASC), falso para descendente (DESC).
     */
    record OrderItem(Expr expression, boolean isAsc) {}

    /**
     * Representa uma declaração SQL SELECT completa.
     *
     * @param projections A lista de colunas e expressões a serem retornadas.
     * @param table O token que representa o nome da tabela (arquivo CSV).
     * @param whereClause A expressão da cláusula WHERE, ou nulo se não houver.
     * @param orderBy A lista de termos da cláusula ORDER BY, ou nulo.
     * @param limit A expressão da cláusula LIMIT, ou nulo.
     */
    record Select(
        List<SelectItem> projections,
        Token table,
        Expr whereClause,
        List<OrderItem> orderBy,
        Expr limit
    ) implements Stmt {}
}