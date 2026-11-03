package printscript.runner.version

import printscript.semantic.SemanticExpressionHandler
import printscript.semantic.SemanticStatementHandler
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.handler.statement.AssignmentHandler
import printscript.semantic.handler.statement.PrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler

internal object PrintScript10 : LanguageDefinition {
    override val semanticStatements: List<SemanticStatementHandler> =
        listOf(
            VariableDeclarationHandler(),
            AssignmentHandler(),
            PrintCallHandler(),
        )

    override val semanticExpressions: List<SemanticExpressionHandler> =
        listOf(
            NumberLiteralHandler(),
            StringLiteralHandler(),
            IdentifierHandler(),
            BinaryExpressionHandler(),
        )
}
