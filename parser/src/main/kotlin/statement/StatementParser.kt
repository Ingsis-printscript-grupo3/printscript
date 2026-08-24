package printscript.parser.statement

import printscript.ast.Statement
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class StatementParser(
    private val stream: TokenStream,
    private val expressionParser: ExpressionParser,
    private val handlers: Map<TokenType, StatementHandler> = DefaultStatementHandlers.map,
) {
    fun parseStatement(): ASTResult<Statement> {
        val token = stream.peek()
        if (token == null) {
            val pos = stream.previous()?.end ?: Position(0, 0)
            return ASTResult.Failure("Syntax Error: Unexpected end of input.", pos, pos)
        }

        val handler =
            handlers[token.type]
                ?: return ASTResult.Failure(
                    "Syntax Error [Line ${token.start.line}]: Unexpected token '${token.value}'.",
                    token.start,
                    token.end,
                )
        stream.advance()
        return handler.parse(stream, expressionParser)
    }
}
