package com.sqlcsv;

import com.sqlcsv.csv.CsvReader;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {
    @Test
    void testSimpleCsv() throws IOException {
        String content = "a,b,c\n1,2,3";
        Path tempFile = Files.createTempFile("test", ".csv");
        Files.writeString(tempFile, content);

        try (CsvReader reader = new CsvReader(tempFile.toString(), ',', true)) {
            assertArrayEquals(new String[]{"a", "b", "c"}, reader.getHeader());
            assertTrue(reader.hasNext());
            assertArrayEquals(new String[]{"1", "2", "3"}, reader.next());
            assertFalse(reader.hasNext());
        } finally {
            Files.delete(tempFile);
        }
    }

    @Test
    void testQuotedAndEscapedCsv() throws IOException {
        String content = "id,name,description\n1,\"John, Doe\",\"He said \"\"Hi!\"\"\"\n2,Jane,\"A, B, and C\"";
        Path tempFile = Files.createTempFile("test", ".csv");
        Files.writeString(tempFile, content);

        try (CsvReader reader = new CsvReader(tempFile.toString(), ',', true)) {
            assertArrayEquals(new String[]{"id", "name", "description"}, reader.getHeader());
            
            String[] row1 = reader.next();
            assertEquals("1", row1[0]);
            assertEquals("John, Doe", row1[1]);
            assertEquals("He said \"Hi!\"", row1[2]);
            
            String[] row2 = reader.next();
            assertEquals("2", row2[0]);
            assertEquals("Jane", row2[1]);
            assertEquals("A, B, and C", row2[2]);
        } finally {
            Files.delete(tempFile);
        }
    }
}