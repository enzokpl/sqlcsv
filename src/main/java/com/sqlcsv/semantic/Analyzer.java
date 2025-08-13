package com.sqlcsv.semantic;

import com.sqlcsv.ast.Expr;
import com.sqlcsv.ast.Stmt;
import com.sqlcsv.lexer.Token;
import com.sqlcsv.lexer.TokenType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * O analisador semântico, responsável por validar a AST.
 * <p>
 * Suas principais funções são:
 * <ul>
 * <li>Expandir `SELECT *` para a lista de todas as colunas.</li>
 * <li>Verificar se as colunas usadas na query realmente existem no cabeçalho do CSV.</li>
 * </ul>
 */
public class Analyzer implements Expr.Visitor<Void> {
    private final Stmt.Select statement;
    // Usamos um Set para validações rápidas (O(1)) de existência de colunas.
    private final Set<String> availableColumns;
    // E mantemos a lista ordenada para expandir o SELECT * na ordem correta.
    private final List<String> orderedColumns;

    /**
     * Constrói um novo analisador.
     *
     * @param statement A declaração SELECT a ser analisada.
     * @param csvHeader A lista de nomes de colunas do arquivo CSV, na ordem original.
     */
    public Analyzer(Stmt.Select statement, List<String> csvHeader) {
        this.statement = statement;
        this.orderedColumns = csvHeader;
        this.availableColumns = new HashSet<>(csvHeader);
    }

    /**
     * Executa a análise semântica.
     *
     * @return Uma nova declaração {@link Stmt.Select}, possivelmente com a projeção de `*` expandida.
     * @throws SemanticError se alguma validação falhar.
     */
    public Stmt.Select analyze() {
        List<Stmt.SelectItem> analyzedProjections = new ArrayList<>();
        boolean isSelectStar = statement.projections().size() == 1 && statement.projections().get(0).expression() == null;

        if (isSelectStar) {
            for (String colName : this.orderedColumns) {
                var identifier = new Expr.Identifier(new Token(TokenType.IDENTIFIER, colName, colName, 0, 0));
                analyzedProjections.add(new Stmt.SelectItem(identifier, colName));
            }
        } else {
            analyzedProjections.addAll(statement.projections());
        }

        for (Stmt.SelectItem item : analyzedProjections) {
            if (item.expression() != null) {
                resolve(item.expression());
            }
        }

        if (statement.whereClause() != null) {
            resolve(statement.whereClause());
        }

        if (statement.orderBy() != null) {
            for (Stmt.OrderItem item : statement.orderBy()) {
                resolve(item.expression());
            }
        }

        return new Stmt.Select(analyzedProjections, statement.table(), statement.whereClause(), statement.orderBy(), statement.limit());
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression());
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitIdentifierExpr(Expr.Identifier expr) {
        String colName = expr.name().lexeme();
        if (!availableColumns.contains(colName)) {
            throw new SemanticError("Column '" + colName + "' not found in table '" + statement.table().lexeme() + "'. Available columns: " + availableColumns);
        }
        return null;
    }
}