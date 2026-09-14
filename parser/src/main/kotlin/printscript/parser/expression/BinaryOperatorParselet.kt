package printscript.parser.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class BinaryOperatorParselet(override val precedence: Int) : InfixParselet {
    override fun parse(
        left: Expression,
        operatorToken: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val right =
            when (val result = expressionParser.parseExpression(precedence + 1)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }
        return ASTResult.Success(BinaryExpression(left, operatorToken.type, right, left.position))
    }
}
