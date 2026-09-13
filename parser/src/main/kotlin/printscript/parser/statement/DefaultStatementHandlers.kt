package printscript.parser.statement

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler

object DefaultStatementHandlers {
    fun map(version: LanguageVersion = LanguageVersion.V1_1): Map<TokenType, StatementHandler> =
        when (version) {
            LanguageVersion.V1_0 -> handlers10
            LanguageVersion.V1_1 -> handlers10 + handlers11
        }

    // lo que ya parseaba 1.0
    private val handlers10: Map<TokenType, StatementHandler> =
        mapOf(
            TokenType.LET to VariableDeclarationHandler,
            TokenType.PRINTLN to PrintCallHandler,
            TokenType.IDENTIFIER to AssignmentHandler,
        )

    // 1.1 se monta sobre 1.0 y le suma const y el if. En 1.0 esas claves no estan,
    // asi que el StatementParser no encuentra handler y VersionFeatures da el error
    private val handlers11: Map<TokenType, StatementHandler> =
        mapOf(
            TokenType.CONST to VariableDeclarationHandler,
            TokenType.IF to IfStatementHandler,
        )
}
