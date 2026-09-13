package printscript.parser.statement.handlers

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

object AssignmentHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        // el StatementParser consume el identificador antes de despachar el handler
        val nameToken = checkNotNull(stream.previous()) { "the assignment handler runs after its identifier" }

        val assignResult = stream.consume(TokenType.ASSIGN, "Expected '='.")
        if (assignResult is ASTResult.Failure) return assignResult

        val value =
            when (val result = expressionParser.parseExpression()) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult

        return ASTResult.Success(Assignment(nameToken.value, value, nameToken.start))
    }
}
