package com.sqlcsv.cli;

import com.sqlcsv.ast.Stmt;
import com.sqlcsv.csv.CsvReader;
import com.sqlcsv.exec.QueryExecutor;
import com.sqlcsv.exec.RuntimeError;
import com.sqlcsv.lexer.Lexer;
import com.sqlcsv.lexer.Token;
import com.sqlcsv.lexer.TokenType;
import com.sqlcsv.parser.ParseError;
import com.sqlcsv.parser.Parser;
import com.sqlcsv.semantic.Analyzer;
import com.sqlcsv.semantic.SemanticError;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * A classe principal da aplicação de linha de comando (CLI).
 * <p>
 * É responsável por:
 * <ul>
 * <li>Analisar os argumentos da linha de comando.</li>
 * <li>Orquestrar o pipeline: Lexer -> Parser -> Analyzer -> Executor.</li>
 * <li>Imprimir os resultados ou erros no console.</li>
 * </ul>
 */
public class Main {
    /**
     * O ponto de entrada da aplicação.
     *
     * @param args os argumentos da linha de comando.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(64);
        }

        String query = args[0];
        String csvPath = null;
        char separator = ',';
        boolean hasHeader = true;
        String nullStr = "";

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--csv":
                    if (i + 1 < args.length) csvPath = args[++i];
                    break;
                case "--separator":
                    if (i + 1 < args.length) separator = args[++i].charAt(0);
                    break;
                case "--header":
                    if (i + 1 < args.length) hasHeader = Boolean.parseBoolean(args[++i]);
                    break;
                case "--null-str":
                    if (i + 1 < args.length) nullStr = args[++i];
                    break;
            }
        }

        if (csvPath == null) {
            System.err.println("Error: --csv argument is mandatory.");
            printUsage();
            System.exit(64);
        }

        try {
            run(query, csvPath, separator, hasHeader, nullStr);
        } catch (Lexer.LexerError | ParseError | SemanticError | RuntimeError | IOException e) {
            System.err.println(e.getMessage());
            if (e instanceof ParseError pe) {
                printErrorContext(query, pe.token);
            }
            System.exit(65);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(70);
        }
    }

    private static void run(String query, String csvPath, char separator, boolean hasHeader, String nullStr) throws IOException {
        long startTime = System.currentTimeMillis();

        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);
        Stmt.Select selectStmt = parser.parse();

        try (CsvReader csvReader = new CsvReader(csvPath, separator, hasHeader)) {
            String tableNameFromFile = Paths.get(csvPath).getFileName().toString().replaceFirst("[.][^.]+$", "");
            if (!selectStmt.table().lexeme().equalsIgnoreCase(tableNameFromFile)) {
                throw new SemanticError("Table name in query '" + selectStmt.table().lexeme() + "' does not match CSV file name '" + tableNameFromFile + "'.");
            }

            Analyzer analyzer = new Analyzer(selectStmt, Arrays.asList(csvReader.getHeader()));
            Stmt.Select analyzedStmt = analyzer.analyze();

            QueryExecutor executor = new QueryExecutor(analyzedStmt, csvReader, nullStr);
            QueryExecutor.ResultTable result = executor.execute();

            long endTime = System.currentTimeMillis();

            printTable(result);
            System.err.printf("Rows: %d (%d ms)\n", result.rows().size(), endTime - startTime);
        }
    }

    private static void printTable(QueryExecutor.ResultTable table) {
        if (table.rows().isEmpty()) {
            System.out.println("Query returned no results.");
            return;
        }

        List<String> headers = table.header();
        List<Object[]> rows = table.rows();

        int[] maxWidths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            maxWidths[i] = headers.get(i).length();
        }

        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                String val = row[i] == null ? "NULL" : row[i].toString();
                if (val.length() > maxWidths[i]) {
                    maxWidths[i] = val.length();
                }
            }
        }

        printSeparator(maxWidths);
        System.out.print("|");
        for (int i = 0; i < headers.size(); i++) {
            System.out.printf(" %-" + maxWidths[i] + "s |", headers.get(i));
        }
        System.out.println();
        printSeparator(maxWidths);

        for (Object[] row : rows) {
            System.out.print("|");
            for (int i = 0; i < row.length; i++) {
                String val = row[i] == null ? "NULL" : row[i].toString();
                if (row[i] instanceof Double d) {
                    System.out.printf(" %" + maxWidths[i] + ".1f |", d);
                } else {
                    System.out.printf(" %-" + maxWidths[i] + "s |", val);
                }
            }
            System.out.println();
        }
        printSeparator(maxWidths);
    }

    private static void printSeparator(int[] maxWidths) {
        System.out.print("+");
        for (int width : maxWidths) {
            for(int i=0; i < width + 2; i++) System.out.print("-");
            System.out.print("+");
        }
        System.out.println();
    }

    private static void printErrorContext(String source, Token token) {
        if (token.type() == TokenType.EOF) {
            System.err.println(" at end of file.");
            return;
        }
        String[] lines = source.split("\n");
        int lineIndex = token.line() - 1;
        if (lineIndex < lines.length) {
            String line = lines[lineIndex];
            System.err.println("\n" + line);
            for (int i = 0; i < token.col() - 1; i++) {
                System.err.print(" ");
            }
            System.err.println("^-- Here");
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar sqlcsv.jar \"<SQL_QUERY>\" --csv <path> [OPTIONS]");
        System.err.println("Options:");
        System.err.println("  --csv <path>             (Required) Path to the CSV file.");
        System.err.println("  --separator <char>       (Optional) CSV delimiter. Default: ,");
        System.err.println("  --header <true|false>    (Optional) CSV has header. Default: true");
        System.err.println("  --null-str <string>      (Optional) String to treat as NULL. Default: \"\"");
    }
}