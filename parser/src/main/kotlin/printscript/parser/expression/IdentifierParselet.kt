package printscript.parser.expression

import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

object IdentifierParselet : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> = ASTResult.Success(Identifier(token.value, token.start))
}
