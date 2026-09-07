package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

fun interface PrefixParselet {
    fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression>
}
