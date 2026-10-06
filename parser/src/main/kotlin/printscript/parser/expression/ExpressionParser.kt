package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class ExpressionParser(
    private val stream: TokenStream,
    private val prefixParselets: Map<TokenType, PrefixParselet>,
    private val infixParselets: Map<TokenType, InfixParselet>,
) {
    fun parseExpression(minPrecedence: Int = 0): ASTResult<Expression> {
        var left =
            when (val result = parsePrimary()) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        while (true) {
            val token = stream.peek() ?: break
            val parselet = infixParselets[token.type] ?: break
            if (parselet.precedence < minPrecedence) break

            stream.advance()
            val operatorToken = stream.previous() ?: break
            left =
                when (val result = parselet.parse(left, operatorToken, stream, this)) {
                    is ASTResult.Failure -> return result
                    is ASTResult.Success -> result.value
                }
        }
        return ASTResult.Success(left)
    }

    // el token de fin de archivo tiene value vacio: sin esto el mensaje sale "found ''"
    private fun describe(token: Token): String =
        if (token.type == TokenType.EOF) "end of input" else "'" + token.value + "'"

    private fun parsePrimary(): ASTResult<Expression> {
        val token = stream.peek()
        if (token == null) {
            // sin token previo estamos al principio del archivo
            val pos = stream.previous()?.end ?: Position(1, 1)
            return ASTResult.Failure("Expected a value or expression, found end of input.", pos, pos)
        }

        val parselet =
            prefixParselets[token.type]
                ?: return ASTResult.Failure(
                    "Expected a value or expression, found ${describe(token)}.",
                    token.start,
                    token.end,
                )
        stream.advance()
        return parselet.parse(token, stream, this)
    }
}
