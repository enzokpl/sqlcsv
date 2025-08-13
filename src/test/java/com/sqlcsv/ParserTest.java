package com.sqlcsv;

import com.sqlcsv.ast.Expr;
import com.sqlcsv.ast.Stmt;
import com.sqlcsv.lexer.Lexer;
import com.sqlcsv.parser.ParseError;
import com.sqlcsv.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    
    private Stmt.Select parseQuery(String query) {
        Lexer lexer = new Lexer(query);
        Parser parser = new Parser(lexer.scanTokens());
        return parser.parse();
    }

    @Test
    void testFullStatement() {
        String query = "SELECT id, price * 1.1 AS new_price FROM sales WHERE amount > 100 ORDER BY date DESC, id ASC LIMIT 50";
        Stmt.Select stmt = parseQuery(query);
        
        assertEquals(2, stmt.projections().size());
        assertEquals("new_price", stmt.projections().get(1).alias());
        assertNotNull(stmt.whereClause());
        assertEquals(2, stmt.orderBy().size());
        assertFalse(stmt.orderBy().get(0).isAsc()); // DESC
        assertTrue(stmt.orderBy().get(1).isAsc());  // ASC
        assertNotNull(stmt.limit());
    }
    
    @Test
    void testSelectStar() {
        String query = "SELECT * FROM my_table";
        Stmt.Select stmt = parseQuery(query);
        assertEquals(1, stmt.projections().size());
        assertNull(stmt.projections().get(0).expression()); // Star is represented by null expression
    }

    @Test
    void testOperatorPrecedence() {
        String query = "SELECT 1 + 2 * 3 FROM t"; // Should be parsed as 1 + (2 * 3)
        Stmt.Select stmt = parseQuery(query);
        Expr.Binary expr = (Expr.Binary) stmt.projections().get(0).expression();

        assertEquals("+", expr.operator().lexeme());
        assertTrue(expr.left() instanceof Expr.Literal);
        assertTrue(expr.right() instanceof Expr.Binary); // * has higher precedence
        
        Expr.Binary multExpr = (Expr.Binary) expr.right();
        assertEquals("*", multExpr.operator().lexeme());
    }

    @Test
    void testMissingFromError() {
        String query = "SELECT id";
        assertThrows(ParseError.class, () -> parseQuery(query));
    }
    
    @Test
    void testMalformedWhere() {
        String query = "SELECT * FROM t WHERE";
         assertThrows(ParseError.class, () -> parseQuery(query));
    }
}