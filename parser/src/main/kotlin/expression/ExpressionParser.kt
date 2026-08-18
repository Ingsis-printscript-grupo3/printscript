package printscript.parser.expression
import printscript.ast.*
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class ExpressionParser(
    private val stream: TokenStream,
    private val infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix
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
        if (stream.match(TokenType.NUMBERLITERAL)) {
            val value = stream.previous()?.value?.toDouble() ?: 0.0
            return ASTResult.Success(NumberLiteral(value))
        }
        if (stream.match(TokenType.STRINGLITERAL)) {
            val value = stream.previous()?.value ?: ""
            return ASTResult.Success(StringLiteral(value))
        }
        if (stream.match(TokenType.IDENTIFIER)) {
            val value = stream.previous()?.value ?: ""
            return ASTResult.Success(Identifier(value))
        }
        if (stream.match(TokenType.LEFTPAREN)) {
            val exprResult = parseExpression()
            if (exprResult is ASTResult.Failure) return exprResult
            
            val consumeResult = stream.consume(TokenType.RIGHTTPAREN, "Expected ')' closing the expression.")
            if (consumeResult is ASTResult.Failure) return consumeResult
            
            return exprResult
        }
        val errorToken = stream.peek()
        val pos = errorToken?.start ?: stream.previous()?.end ?: Position(0, 0)
        return ASTResult.Failure("Syntax Error [Line ${pos.line}]: Expected a value or expression.", pos, errorToken?.end ?: pos)
    }
}
