package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class ExpressionParser(
    private val stream: TokenStream,
    private val prefixParselets: Map<TokenType, PrefixParselet> = DefaultExpressionParselets.prefix(),
    private val infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix,
) {
    fun parseExpression(minPrecedence: Int = 0): ASTResult<Expression> {
        val leftResult = parsePrimary()
        if (leftResult is ASTResult.Failure) return leftResult
        var left = (leftResult as ASTResult.Success).value

        while (true) {
            val token = stream.peek() ?: break
            val parselet = infixParselets[token.type] ?: break
            if (parselet.precedence < minPrecedence) break

            stream.advance()
            val operatorToken = stream.previous() ?: break
            val result = parselet.parse(left, operatorToken, stream, this)
            if (result is ASTResult.Failure) return result
            left = (result as ASTResult.Success).value
        }
        return ASTResult.Success(left)
    }

    private fun parsePrimary(): ASTResult<Expression> {
        val token = stream.peek()
        if (token == null) {
            val pos = stream.previous()?.end ?: Position(0, 0)
            return ASTResult.Failure("Expected a value or expression.", pos, pos)
        }

        val parselet =
            prefixParselets[token.type]
                ?: return ASTResult.Failure("Expected a value or expression.", token.start, token.end)
        stream.advance()
        return parselet.parse(token, stream, this)
    }
}
