package com.sqlcsv.exec;

import com.sqlcsv.ast.Expr;
import com.sqlcsv.ast.Stmt;
import com.sqlcsv.csv.CsvReader;

import java.util.*;
import java.util.stream.Collectors;

/**
 * O motor de execução da query.
 * <br>
 * Orquestra a leitura do CSV, filtragem (WHERE), projeção (SELECT), ordenação (ORDER BY) e limitação (LIMIT).
 */
public class QueryExecutor {

    private final Stmt.Select analyzedStmt;
    private final CsvReader csvReader;
    private final String nullStr;

    /**
     * Tabela de resultados contendo o cabeçalho e as linhas de dados.
     *
     * @param header A lista de nomes de colunas do resultado.
     * @param rows Uma lista de arrays de objetos, onde cada array representa uma linha.
     */
    public record ResultTable(List<String> header, List<Object[]> rows) {}

    /**
     * Constrói um novo executor de queries.
     *
     * @param analyzedStmt A declaração SELECT já validada pelo {@link com.sqlcsv.semantic.Analyzer}.
     * @param csvReader O leitor para o arquivo CSV de dados.
     * @param nullStr A string que deve ser tratada como NULL.
     */
    public QueryExecutor(Stmt.Select analyzedStmt, CsvReader csvReader, String nullStr) {
        this.analyzedStmt = analyzedStmt;
        this.csvReader = csvReader;
        this.nullStr = nullStr;
    }

    /**
     * Executa a query e retorna os resultados.
     *
     * @return um {@link ResultTable} com os dados resultantes.
     */
    public ResultTable execute() {
        List<Object[]> resultRows = new ArrayList<>();
        List<String> resultHeader = analyzedStmt.projections().stream()
            .map(p -> p.alias() != null ? p.alias() : getExpressionAlias(p.expression()))
            .collect(Collectors.toList());

        List<String> csvHeader = Arrays.asList(csvReader.getHeader());

        while (csvReader.hasNext()) {
            String[] currentRow = csvReader.next();
            ExpressionEvaluator whereEvaluator = new ExpressionEvaluator(csvHeader, currentRow, nullStr);

            boolean passes = true;
            if (analyzedStmt.whereClause() != null) {
                Object result = whereEvaluator.evaluate(analyzedStmt.whereClause());
                if (!(result instanceof Boolean && (Boolean) result)) {
                    passes = false;
                }
            }

            if (passes) {
                ExpressionEvaluator projectEvaluator = new ExpressionEvaluator(csvHeader, currentRow, nullStr);
                Object[] projectedRow = new Object[analyzedStmt.projections().size()];
                for (int i = 0; i < analyzedStmt.projections().size(); i++) {
                    projectedRow[i] = projectEvaluator.evaluate(analyzedStmt.projections().get(i).expression());
                }
                resultRows.add(projectedRow);
            }
        }

        if (analyzedStmt.orderBy() != null && !analyzedStmt.orderBy().isEmpty()) {
            sortResults(resultRows, resultHeader);
        }

        long limit = Long.MAX_VALUE;
        if (analyzedStmt.limit() != null) {
            Object limitVal = new ExpressionEvaluator(List.of(), new String[0], nullStr).evaluate(analyzedStmt.limit());
            if (limitVal instanceof Integer) {
                limit = (long) (Integer) limitVal;
            } else if (limitVal instanceof Double) {
                limit = ((Double) limitVal).longValue();
            }
        }

        List<Object[]> finalRows = resultRows.stream().limit(limit).collect(Collectors.toList());

        return new ResultTable(resultHeader, finalRows);
    }

    private void sortResults(List<Object[]> resultRows, List<String> resultHeader) {
        resultRows.sort((row1, row2) -> {
            for (Stmt.OrderItem orderItem : analyzedStmt.orderBy()) {
                Object val1 = evaluateOrderByExpr(orderItem.expression(), resultHeader, row1);
                Object val2 = evaluateOrderByExpr(orderItem.expression(), resultHeader, row2);

                int comparison = compareValues(val1, val2);
                if (comparison != 0) {
                    return orderItem.isAsc() ? comparison : -comparison;
                }
            }
            return 0;
        });
    }

    private Object evaluateOrderByExpr(Expr expr, List<String> header, Object[] row) {
        String[] stringRow = Arrays.stream(row)
            .map(obj -> obj == null ? nullStr : obj.toString())
            .toArray(String[]::new);
        ExpressionEvaluator evaluator = new ExpressionEvaluator(header, stringRow, nullStr);
        return evaluator.evaluate(expr);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object o1, Object o2) {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) return -1;
        if (o2 == null) return 1;

        if (o1 instanceof Number && o2 instanceof Number) {
            return Double.compare(((Number)o1).doubleValue(), ((Number)o2).doubleValue());
        }

        if (o1 instanceof Comparable && o1.getClass().equals(o2.getClass())) {
            return ((Comparable) o1).compareTo(o2);
        }

        return String.valueOf(o1).compareTo(String.valueOf(o2));
    }

    private String getExpressionAlias(Expr expr) {
        if (expr instanceof Expr.Identifier) {
            return ((Expr.Identifier) expr).name().lexeme();
        }
        return expr.toString();
    }
}