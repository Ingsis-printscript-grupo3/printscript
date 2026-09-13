package printscript.parser.expression

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

object BooleanLiteralParselet : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> = ASTResult.Success(BooleanLiteral(token.value.toBoolean(), token.start))
}
