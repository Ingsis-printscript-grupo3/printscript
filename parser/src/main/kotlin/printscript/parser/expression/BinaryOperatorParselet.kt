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
        val rightResult = expressionParser.parseExpression(precedence + 1)
        if (rightResult is ASTResult.Failure) return rightResult
        val right = (rightResult as ASTResult.Success).value
        return ASTResult.Success(BinaryExpression(left, operatorToken.type, right))
    }
}
