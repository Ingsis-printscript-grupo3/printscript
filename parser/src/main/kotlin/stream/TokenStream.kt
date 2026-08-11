package printscript.parser.stream

import printscript.common.Token
import printscript.common.TokenType

class TokenStream(private val tokens: Iterator<Token>) {
    private var currentToken: Token
    private var previousToken: Token? = null

    init {
        if (!tokens.hasNext()) throw RuntimeException("Empty token stream")
        currentToken = tokens.next()
    }

    fun peek(): Token = currentToken

    fun advance(): Token {
        if (!isAtEnd()) {
            previousToken = currentToken
            if (tokens.hasNext()) {
                currentToken = tokens.next()
            }
        }
        return previous()
    }

    fun previous(): Token = previousToken ?: throw RuntimeException("No previous token")

    fun isAtEnd(): Boolean = currentToken.type == TokenType.EOF

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
        throw RuntimeException("Syntax Error [Line ${errorToken.start.line}]: $errorMessage")
    }
}
