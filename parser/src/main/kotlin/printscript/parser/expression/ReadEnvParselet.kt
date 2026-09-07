package printscript.parser.expression

import printscript.ast.Expression
import printscript.ast.ReadEnv
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionGate

class ReadEnvParselet(private val version: LanguageVersion) : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        VersionGate.check("readEnv", LanguageVersion.V1_1, version, token.start, token.end)?.let { return it }

        val leftParenResult = stream.consume(TokenType.LEFTPAREN, "Expected '(' after 'readEnv'.")
        if (leftParenResult is ASTResult.Failure) return leftParenResult

        val argumentResult = expressionParser.parseExpression()
        if (argumentResult is ASTResult.Failure) return argumentResult
        val argument = (argumentResult as ASTResult.Success).value

        val rightParenResult = stream.consume(TokenType.RIGHTPAREN, "Expected ')' closing 'readEnv'.")
        if (rightParenResult is ASTResult.Failure) return rightParenResult

        return ASTResult.Success(ReadEnv(argument, token.start))
    }
}
