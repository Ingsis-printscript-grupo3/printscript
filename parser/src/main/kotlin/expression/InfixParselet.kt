package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

interface InfixParselet {
    val precedence: Int

    fun parse(
        left: Expression,
        operatorToken: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser
    ): ASTResult<Expression>
}
