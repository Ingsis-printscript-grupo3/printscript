package printscript.lexer

import printscript.common.TokenType

object LexerRules {

    val keywords: Map<String, TokenType> = mapOf(
        "let" to TokenType.LET,
        "println" to TokenType.PRINTLN,
        "number" to TokenType.NUMBERTYPE,
        "string" to TokenType.STRINGTYPE,
    )

    val symbols: Map<Char, TokenType> = mapOf(
        '+' to TokenType.PLUS,
        '-' to TokenType.MINUS,
        '*' to TokenType.MULTIPLY,
        '/' to TokenType.DIVIDE,
        '=' to TokenType.ASSIGN,
        ':' to TokenType.COLON,
        ';' to TokenType.SEMICOLON,
        '(' to TokenType.LEFTPAREN,
        ')' to TokenType.RIGHTPAREN,
    )

    fun isQuote(c: Char): Boolean = c == '"' || c == '\''

    fun isIdentifierStart(c: Char): Boolean = c.isLetter() || c == '_'

    fun isIdentifierPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}