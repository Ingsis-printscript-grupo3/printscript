package printscript.parser.expression

import printscript.ast.Expression
import printscript.ast.StringLiteral
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

object StringLiteralParselet : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> = ASTResult.Success(StringLiteral(token.value, token.start))
}
