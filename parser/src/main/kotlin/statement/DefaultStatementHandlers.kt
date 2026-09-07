package printscript.parser.statement

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler

object DefaultStatementHandlers {
    fun map(version: LanguageVersion = LanguageVersion.V1_1): Map<TokenType, StatementHandler> {
        val variableDeclarationHandler = VariableDeclarationHandler(version)
        return mapOf(
            TokenType.LET to variableDeclarationHandler,
            TokenType.CONST to variableDeclarationHandler,
            TokenType.PRINTLN to PrintCallHandler,
            TokenType.IDENTIFIER to AssignmentHandler,
            TokenType.IF to IfStatementHandler(version),
        )
    }
}
