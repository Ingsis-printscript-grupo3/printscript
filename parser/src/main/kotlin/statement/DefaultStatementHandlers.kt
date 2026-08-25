package printscript.parser.statement

import printscript.common.TokenType
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler

object DefaultStatementHandlers {
    val map: Map<TokenType, StatementHandler> = mapOf(
        TokenType.LET to VariableDeclarationHandler,
        TokenType.PRINTLN to PrintCallHandler,
        TokenType.IDENTIFIER to AssignmentHandler,
    )
}
