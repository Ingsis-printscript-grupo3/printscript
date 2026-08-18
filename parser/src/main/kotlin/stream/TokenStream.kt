package printscript.parser.stream

import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.common.Position

class TokenStream(private val tokens: Iterator<Token>) {
    private var currentToken: Token? = null
    private var previousToken: Token? = null

    init {
        if (tokens.hasNext()) {
            currentToken = tokens.next()
        }
    }

    fun peek(): Token? = currentToken

    fun advance(): Token? {
        if (!isAtEnd()) {
            previousToken = currentToken
            if (tokens.hasNext()) {
                currentToken = tokens.next()
            } else {
                currentToken = null
            }
        }
        return previous()
    }

    fun previous(): Token? = previousToken

    fun isAtEnd(): Boolean = currentToken?.type == TokenType.EOF || currentToken == null

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek()?.type == type
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

    fun consume(type: TokenType, errorMessage: String): ASTResult<Token> {
        if (check(type)) {
            val token = advance()
            if (token != null) return ASTResult.Success(token)
        }
        val errorToken = peek()
        val pos = errorToken?.start ?: previousToken?.end ?: Position(0, 0)
        return ASTResult.Failure(errorMessage, pos, errorToken?.end ?: pos)
    }
}
