package printscript.lexer

import printscript.common.TokenType

// las keywords no estan aca: cambian con la version y las recibe LexerFactory
object LexerRules {
    val symbols: Map<Char, TokenType> =
        mapOf(
            '+' to TokenType.PLUS,
            '-' to TokenType.MINUS,
            '*' to TokenType.MULTIPLY,
            '/' to TokenType.DIVIDE,
            '=' to TokenType.ASSIGN,
            ':' to TokenType.COLON,
            ';' to TokenType.SEMICOLON,
            '(' to TokenType.LEFTPAREN,
            ')' to TokenType.RIGHTPAREN,
            '{' to TokenType.LEFTBRACE,
            '}' to TokenType.RIGHTBRACE,
        )

    fun isQuote(c: Char): Boolean = c == '"' || c == '\''

    fun isIdentifierStart(c: Char): Boolean = c.isLetter() || c == '_'

    fun isIdentifierPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}
