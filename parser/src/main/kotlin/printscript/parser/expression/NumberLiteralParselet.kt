package printscript.parser.expression

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

object NumberLiteralParselet : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> = ASTResult.Success(NumberLiteral(token.value.toDouble(), token.start))
}
