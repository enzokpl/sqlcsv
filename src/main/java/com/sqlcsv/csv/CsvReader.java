package com.sqlcsv.csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Um leitor de arquivos CSV que implementa um iterador sobre as linhas do arquivo.
 * <p>
 * Suporta campos entre aspas, quebras de linha dentro dos campos e escapes de aspas duplas ("").
 * Implementa {@link AutoCloseable} para ser usado em blocos try-with-resources.
 */
public class CsvReader implements Iterator<String[]>, AutoCloseable {
    private final BufferedReader reader;
    private final char separator;
    private String[] header;
    private String nextLine;

    /**
     * Constrói um leitor de CSV para um arquivo específico.
     *
     * @param filePath O caminho para o arquivo CSV.
     * @param separator O caractere separador de colunas.
     * @param hasHeader Se a primeira linha deve ser tratada como cabeçalho.
     * @throws IOException se ocorrer um erro ao abrir ou ler o arquivo.
     */
    public CsvReader(String filePath, char separator, boolean hasHeader) throws IOException {
        this.reader = new BufferedReader(new FileReader(filePath));
        this.separator = separator;
        if (hasHeader) {
            this.header = parseLine(this.reader.readLine());
        }
        // Prepara a primeira linha de dados para o iterador.
        this.nextLine = this.reader.readLine();
    }

    /**
     * Retorna o cabeçalho do arquivo CSV.
     *
     * @return um array de strings com os nomes das colunas, ou nulo se não houver cabeçalho.
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * Verifica se há mais linhas a serem lidas no arquivo.
     *
     * @return true se houver mais linhas, falso caso contrário.
     */
    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    /**
     * Retorna a próxima linha do CSV como um array de strings.
     *
     * @return um array de strings representando os campos da linha.
     * @throws NoSuchElementException se não houver mais linhas.
     */
    @Override
    public String[] next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more lines in CSV file.");
        }
        try {
            String[] currentRecord = parseLine(nextLine);
            nextLine = reader.readLine();
            return currentRecord;
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file", e);
        }
    }

    private String[] parseLine(String line) {
        if (line == null || line.isEmpty()) {
            return new String[0];
        }

        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Verifica se é uma aspa de escape ("")
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++; // Pula a próxima aspa
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == separator) {
                    fields.add(currentField.toString());
                    currentField.setLength(0);
                } else {
                    currentField.append(c);
                }
            }
        }
        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Fecha o leitor do arquivo.
     *
     * @throws IOException se ocorrer um erro ao fechar o leitor.
     */
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}