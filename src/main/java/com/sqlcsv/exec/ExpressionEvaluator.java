package com.sqlcsv.exec;

import com.sqlcsv.ast.Expr;
import com.sqlcsv.lexer.TokenType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Um visitor da AST que avalia o valor de uma expressão para uma determinada linha de dados.
 */
public class ExpressionEvaluator implements Expr.Visitor<Object> {
    private final Map<String, Integer> columnMap;
    private final String[] currentRow;
    private final String nullStr;

    /**
     * Constrói um novo avaliador de expressões.
     *
     * @param header A lista de nomes de colunas (para mapear nomes a índices).
     * @param currentRow A linha de dados atual, cujos valores serão usados para resolver identificadores.
     * @param nullStr A string que deve ser tratada como NULL.
     */
    public ExpressionEvaluator(List<String> header, String[] currentRow, String nullStr) {
        this.columnMap = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            this.columnMap.put(header.get(i), i);
        }
        this.currentRow = currentRow;
        this.nullStr = nullStr;
    }

    /**
     * Avalia uma expressão da AST.
     *
     * @param expr O nó da expressão a ser avaliado.
     * @return O resultado da avaliação (pode ser um Double, Boolean, String ou null).
     */
    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left());
        Object right = evaluate(expr.right());

        if (left == null || right == null) {
            if (expr.operator().type() == TokenType.EQUAL) {
                return left == null && right == null;
            }
            if (expr.operator().type() == TokenType.NOT_EQUAL || expr.operator().type() == TokenType.BANG_EQUAL) {
                return left != right;
            }
            return null; // Propaga NULL para a maioria das operações.
        }

        switch (expr.operator().type()) {
            case MINUS:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) - toDouble(right);
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) + toDouble(right);
            case SLASH:
                checkNumberOperands(expr.operator(), left, right);
                if (toDouble(right) == 0.0) {
                    throw new RuntimeError(expr.operator(), "Division by zero.");
                }
                return toDouble(left) / toDouble(right);
            case STAR:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) * toDouble(right);
            case PERCENT:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) % toDouble(right);

            case GREATER:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) > toDouble(right);
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) >= toDouble(right);
            case LESS:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) < toDouble(right);
            case LESS_EQUAL:
                checkNumberOperands(expr.operator(), left, right);
                return toDouble(left) <= toDouble(right);

            case EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
            case NOT_EQUAL:
                return !isEqual(left, right);

            case AND:
                return isTruthy(left) && isTruthy(right);
            case OR:
                return isTruthy(left) || isTruthy(right);
        }

        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right());

        switch (expr.operator().type()) {
            case NOT:
                return !isTruthy(right);
            case MINUS:
                if (right == null) {
                    return null;
                }
                checkNumberOperand(expr.operator(), right);
                return -toDouble(right);
        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression());
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visitIdentifierExpr(Expr.Identifier expr) {
        String colName = expr.name().lexeme();
        if (!columnMap.containsKey(colName)) {
            throw new RuntimeError(expr.name(), "Undefined column: " + colName);
        }
        int colIndex = columnMap.get(colName);
        if (colIndex >= currentRow.length) {
            return null;
        }

        String rawValue = currentRow[colIndex];
        return parseValue(rawValue);
    }

    private Object parseValue(String rawValue) {
        if (rawValue == null || rawValue.equals(nullStr)) {
            return null;
        }
        if (rawValue.equalsIgnoreCase("true")) {
            return true;
        }
        if (rawValue.equalsIgnoreCase("false")) {
            return false;
        }
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            return rawValue;
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }

        if (a instanceof Number && b instanceof Number) {
            return toDouble(a).equals(toDouble(b));
        }

        return a.equals(b);
    }

    private Double toDouble(Object o) {
        if (o instanceof Double) {
            return (Double) o;
        }
        if (o instanceof Integer) {
            return ((Integer) o).doubleValue();
        }
        if (o instanceof String) {
            return Double.parseDouble((String) o);
        }
        return (Double) o;
    }

    private String stringify(Object object) {
        if (object == null) {
            return "NULL";
        }
        return object.toString();
    }

    private void checkNumberOperand(com.sqlcsv.lexer.Token operator, Object operand) {
        if (operand instanceof Number) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(com.sqlcsv.lexer.Token operator, Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
}