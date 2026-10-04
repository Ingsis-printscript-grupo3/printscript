package printscript.parser.statement

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.statement.handlers.AssignmentHandler
import printscript.parser.statement.handlers.IfStatementHandler
import printscript.parser.statement.handlers.PrintCallHandler
import printscript.parser.statement.handlers.VariableDeclarationHandler

object DefaultStatementHandlers {
    fun map(version: LanguageVersion): Map<TokenType, StatementHandler> =
        when (version) {
            LanguageVersion.V1_0 -> handlers10
            LanguageVersion.V1_1 -> handlers10 + handlers11
        }

    // lo que ya parseaba 1.0
    private val types10 = setOf(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)
    private val types11 = types10 + TokenType.BOOLEANTYPE

    private val handlers10: Map<TokenType, StatementHandler> =
        mapOf(
            TokenType.LET to VariableDeclarationHandler(types10),
            TokenType.PRINTLN to PrintCallHandler,
            TokenType.IDENTIFIER to AssignmentHandler,
        )

    // 1.1 se monta sobre 1.0 y le suma const y el if
    private val handlers11: Map<TokenType, StatementHandler> =
        mapOf(
            TokenType.LET to VariableDeclarationHandler(types11),
            TokenType.CONST to VariableDeclarationHandler(types11),
            TokenType.IF to IfStatementHandler,
        )
}
