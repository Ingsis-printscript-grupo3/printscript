package printscript.lexer

import printscript.common.LanguageVersion
import printscript.common.TokenType

object LexerRules {
    val keywords10: Map<String, TokenType> =
        mapOf(
            "let" to TokenType.LET,
            "println" to TokenType.PRINTLN,
            "number" to TokenType.NUMBERTYPE,
            "string" to TokenType.STRINGTYPE,
        )

    val keywords11: Map<String, TokenType> =
        keywords10 +
            mapOf(
                "const" to TokenType.CONST,
                "boolean" to TokenType.BOOLEANTYPE,
                "true" to TokenType.BOOLEANLITERAL,
                "false" to TokenType.BOOLEANLITERAL,
                "if" to TokenType.IF,
                "else" to TokenType.ELSE,
                "readInput" to TokenType.READINPUT,
                "readEnv" to TokenType.READENV,
            )

    fun keywordsFor(version: LanguageVersion): Map<String, TokenType> =
        when (version) {
            LanguageVersion.V1_0 -> keywords10
            LanguageVersion.V1_1 -> keywords11
        }

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
