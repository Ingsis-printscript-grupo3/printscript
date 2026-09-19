package printscript.parser.statement

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionFeatures

class StatementParser(
    private val stream: TokenStream,
    private val expressionParser: ExpressionParser,
    internal val version: LanguageVersion,
    private val handlers: Map<TokenType, StatementHandler> = DefaultStatementHandlers.map(version),
) {
    fun parseStatement(): ASTResult<Statement> {
        val token = stream.peek()
        if (token == null) {
            // sin token previo estamos al principio del archivo
            val pos = stream.previous()?.end ?: Position(1, 1)
            return ASTResult.Failure("Unexpected end of input.", pos, pos)
        }

        val handler = handlers[token.type]
        if (handler == null) {
            // sin handler puede ser una feature de otra version o un token que no abre
            // ninguna sentencia: quien distingue los dos casos es VersionFeatures
            return VersionFeatures.unavailable(token, version)
                ?: ASTResult.Failure("Unexpected token '${token.value}'.", token.start, token.end)
        }
        stream.advance()
        return handler.parse(stream, expressionParser, this)
    }
}
