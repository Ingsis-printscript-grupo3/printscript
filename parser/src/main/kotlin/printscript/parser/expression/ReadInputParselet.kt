package printscript.parser.expression

import printscript.ast.Expression
import printscript.ast.ReadInput
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

object ReadInputParselet : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val leftParenResult = stream.consume(TokenType.LEFTPAREN, "Expected '(' after 'readInput'.")
        if (leftParenResult is ASTResult.Failure) return leftParenResult

        val argument =
            when (val result = expressionParser.parseExpression()) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val rightParenResult = stream.consume(TokenType.RIGHTPAREN, "Expected ')' closing 'readInput'.")
        if (rightParenResult is ASTResult.Failure) return rightParenResult

        return ASTResult.Success(ReadInput(argument, token.start))
    }
}
