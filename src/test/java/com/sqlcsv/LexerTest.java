package com.sqlcsv;

import com.sqlcsv.lexer.Lexer;
import com.sqlcsv.lexer.Token;
import com.sqlcsv.lexer.TokenType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    @Test
    void testBasicSelect() {
        String query = "SELECT id, name FROM users";
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.SELECT, tokens.get(0).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals(TokenType.COMMA, tokens.get(2).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type());
        assertEquals(TokenType.FROM, tokens.get(4).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(5).type());
        assertEquals(TokenType.EOF, tokens.get(6).type());
    }

    @Test
    void testOperators() {
        String query = "<> != = > >= < <=";
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.NOT_EQUAL, tokens.get(0).type());
        assertEquals(TokenType.BANG_EQUAL, tokens.get(1).type());
        assertEquals(TokenType.EQUAL, tokens.get(2).type());
        assertEquals(TokenType.GREATER, tokens.get(3).type());
        assertEquals(TokenType.GREATER_EQUAL, tokens.get(4).type());
        assertEquals(TokenType.LESS, tokens.get(5).type());
        assertEquals(TokenType.LESS_EQUAL, tokens.get(6).type());
    }

    @Test
    void testLiterals() {
        String query = "'hello world' 123 45.67";
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.STRING, tokens.get(0).type());
        assertEquals("hello world", tokens.get(0).literal());
        assertEquals(TokenType.INTEGER, tokens.get(1).type());
        assertEquals(123, tokens.get(1).literal());
        assertEquals(TokenType.DOUBLE, tokens.get(2).type());
        assertEquals(45.67, tokens.get(2).literal());
    }
    
    @Test
    void testUnterminatedString() {
        String query = "SELECT 'oops";
        Lexer lexer = new Lexer(query);
        assertThrows(Lexer.LexerError.class, lexer::scanTokens);
    }
}