package com.sqlcsv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sqlcsv.ast.Stmt;
import com.sqlcsv.csv.CsvReader;
import com.sqlcsv.exec.QueryExecutor;
import com.sqlcsv.lexer.Lexer;
import com.sqlcsv.lexer.Token;
import com.sqlcsv.parser.Parser;
import com.sqlcsv.semantic.Analyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutorTest {

    // Helper method to run a query against a CSV string
    private QueryExecutor.ResultTable execute(String queryFormat, String csvContent) throws IOException {
        Path tempFile = Files.createTempFile("test_exec_", ".csv");
        String tableName = tempFile.getFileName().toString().replaceFirst("[.][^.]+$", "");
        String query = String.format(queryFormat, tableName);

        System.out.println("\n--- Running Test ---");
        System.out.println("Query: " + query);
        System.out.println("CSV Content: \n" + csvContent);

        Files.writeString(tempFile, csvContent);

        try (CsvReader reader = new CsvReader(tempFile.toString(), ',', true)) {
            Lexer lexer = new Lexer(query);
            List<Token> tokens = lexer.scanTokens();
            // DEBUG LOG: Print the token stream
            System.out.println("[Lexer DEBUG] Tokens: " + tokens);

            Parser parser = new Parser(tokens);
            Stmt.Select parsedStmt = parser.parse();

            Analyzer analyzer = new Analyzer(parsedStmt, Arrays.asList(reader.getHeader()));
            Stmt.Select analyzedStmt = analyzer.analyze();

            QueryExecutor executor = new QueryExecutor(analyzedStmt, reader, "");
            return executor.execute();
        } finally {
            Files.delete(tempFile);
        }
    }

    @Test
    void testFilterAndProject() throws IOException {
        String csv = "id,city,sales\n1,NY,150.0\n2,SF,90.0\n3,NY,200.0";
        // Use %s as a placeholder for the dynamic table name
        String query = "SELECT city, sales * 1.1 AS new_sales FROM %s WHERE sales > 100.0";

        QueryExecutor.ResultTable result = execute(query, csv);

        assertEquals(2, result.rows().size());
        assertEquals(List.of("city", "new_sales"), result.header());

        // Check first result row
        assertEquals("NY", result.rows().get(0)[0]);
        assertEquals(165.0, (Double) result.rows().get(0)[1], 0.001);

        // Check second result row
        assertEquals("NY", result.rows().get(1)[0]);
        assertEquals(220.0, (Double) result.rows().get(1)[1], 0.001);
    }

    @Test
    void testOrderByAndLimit() throws IOException {
        String csv = "id,val\n1,30\n2,10\n3,20";
        String query = "SELECT id, val FROM %s ORDER BY val DESC LIMIT 2";

        QueryExecutor.ResultTable result = execute(query, csv);

        assertEquals(2, result.rows().size());

        // Row 1 (highest val)
        assertEquals(30.0, result.rows().get(0)[1]);
        // Row 2 (second highest val)
        assertEquals(20.0, result.rows().get(1)[1]);
    }

    @Test
    void testSelectStarExpansion() throws IOException {
        String csv = "id,name,active\n1,A,true";
        String query = "SELECT * FROM %s WHERE active = true";

        QueryExecutor.ResultTable result = execute(query, csv);

        assertEquals(1, result.rows().size());
        assertEquals(List.of("id", "name", "active"), result.header());
        assertEquals(1.0, result.rows().get(0)[0]);
        assertEquals("A", result.rows().get(0)[1]);
        assertEquals(true, result.rows().get(0)[2]);
    }
}