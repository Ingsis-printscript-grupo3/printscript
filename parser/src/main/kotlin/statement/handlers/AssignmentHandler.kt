package printscript.parser.statement.handlers

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.stream.TokenStream

object AssignmentHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Statement> {
        val nameToken =
            stream.previous()
                ?: return ASTResult.Failure("Expected identifier", Position(0, 0), Position(0, 0))

        val assignResult = stream.consume(TokenType.ASSIGN, "Expected '='.")
        if (assignResult is ASTResult.Failure) return assignResult

        val exprResult = expressionParser.parseExpression()
        if (exprResult is ASTResult.Failure) return exprResult
        val value = (exprResult as ASTResult.Success).value

        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult

        return ASTResult.Success(Assignment(nameToken.value, value, nameToken.start))
    }
}
