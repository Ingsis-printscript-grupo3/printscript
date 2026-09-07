package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

object ParenthesizedExpressionParselet : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val exprResult = expressionParser.parseExpression()
        if (exprResult is ASTResult.Failure) return exprResult

        val consumeResult = stream.consume(TokenType.RIGHTPAREN, "Expected ')' closing the expression.")
        if (consumeResult is ASTResult.Failure) return consumeResult

        return exprResult
    }
}
