package printscript.parser.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class UnaryMinusParselet(
    val precedence: Int = DefaultExpressionParselets.PREC_UNARY,
) : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val nextToken = stream.peek()
        if (nextToken != null && nextToken.type == TokenType.NUMBERLITERAL) {
            stream.advance()
            val value = nextToken.value.toDouble()
            val folded = if (value == 0.0) 0.0 else -value
            return ASTResult.Success(NumberLiteral(folded, token.start))
        }

        val operand =
            when (val result = expressionParser.parseExpression(precedence)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        return ASTResult.Success(
            BinaryExpression(
                left = NumberLiteral(0.0, token.start),
                operator = TokenType.MINUS,
                right = operand,
                position = token.start,
            ),
        )
    }
}
