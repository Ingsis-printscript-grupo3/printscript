package printscript.parser.expression

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionGate

class BooleanLiteralParselet(private val version: LanguageVersion) : PrefixParselet {
    override fun parse(
        token: Token,
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        VersionGate.check("boolean literals", LanguageVersion.V1_1, version, token.start, token.end)?.let { return it }
        return ASTResult.Success(BooleanLiteral(token.value.toBoolean(), token.start))
    }
}
