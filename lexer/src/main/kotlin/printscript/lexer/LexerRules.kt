package printscript.lexer

import printscript.common.TokenType

object LexerRules {
    val keywords: Map<String, TokenType> =
        mapOf(
            "let" to TokenType.LET,
            "println" to TokenType.PRINTLN,
            "number" to TokenType.NUMBERTYPE,
            "string" to TokenType.STRINGTYPE,
            "const" to TokenType.CONST,
            "boolean" to TokenType.BOOLEANTYPE,
            "true" to TokenType.BOOLEANLITERAL,
            "false" to TokenType.BOOLEANLITERAL,
            "if" to TokenType.IF,
            "else" to TokenType.ELSE,
            "readInput" to TokenType.READINPUT,
            "readEnv" to TokenType.READENV,
        )

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
