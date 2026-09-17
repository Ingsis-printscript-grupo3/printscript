package printscript.parser.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class BinaryOperatorParselet(
    override val precedence: Int,
    val isRightAssociative: Boolean = false,
) : InfixParselet {
    override fun parse(
        left: Expression,
        operatorToken: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val nextPrecedence = if (isRightAssociative) precedence else precedence + 1
        val right =
            when (val result = expressionParser.parseExpression(nextPrecedence)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }
        return ASTResult.Success(BinaryExpression(left, operatorToken.type, right, left.position))
    }
}
