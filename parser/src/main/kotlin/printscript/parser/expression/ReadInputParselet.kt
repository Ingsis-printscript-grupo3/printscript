package printscript.parser.expression

import printscript.ast.Expression
import printscript.ast.ReadInput
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionGate

class ReadInputParselet(private val version: LanguageVersion) : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        VersionGate.check("readInput", LanguageVersion.V1_1, version, token.start, token.end)?.let { return it }

        val leftParenResult = stream.consume(TokenType.LEFTPAREN, "Expected '(' after 'readInput'.")
        if (leftParenResult is ASTResult.Failure) return leftParenResult

        val argumentResult = expressionParser.parseExpression()
        if (argumentResult is ASTResult.Failure) return argumentResult
        val argument = (argumentResult as ASTResult.Success).value

        val rightParenResult = stream.consume(TokenType.RIGHTPAREN, "Expected ')' closing 'readInput'.")
        if (rightParenResult is ASTResult.Failure) return rightParenResult

        return ASTResult.Success(ReadInput(argument, token.start))
    }
}
