package printscript.parser.stream

import printscript.common.Token
import printscript.common.TokenType

class TokenStream(private val tokens: List<Token>) {
    private var current = 0

    fun peek(): Token = tokens[current]

    fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    fun previous(): Token = tokens[current - 1]

    fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    fun consume(type: TokenType, errorMessage: String): Token {
        if (check(type)) return advance()
        val errorToken = peek()
        throw RuntimeException("Error Sintáctico [Línea ${errorToken.start.line}]: $errorMessage")
    }
}
