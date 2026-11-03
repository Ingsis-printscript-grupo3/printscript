package printscript.runner.version

import printscript.semantic.SemanticExpressionHandler
import printscript.semantic.SemanticStatementHandler
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.BooleanLiteralHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.ReadEnvHandler
import printscript.semantic.handler.expression.ReadInputHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.handler.statement.AssignmentHandler
import printscript.semantic.handler.statement.BlockHandler
import printscript.semantic.handler.statement.IfStatementHandler
import printscript.semantic.handler.statement.PrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler

internal object PrintScript11 : LanguageDefinition {
    override val semanticStatements: List<SemanticStatementHandler> =
        listOf(
            VariableDeclarationHandler(),
            AssignmentHandler(),
            PrintCallHandler(),
            IfStatementHandler(),
            BlockHandler(),
        )

    override val semanticExpressions: List<SemanticExpressionHandler> =
        listOf(
            NumberLiteralHandler(),
            StringLiteralHandler(),
            BooleanLiteralHandler(),
            IdentifierHandler(),
            BinaryExpressionHandler(),
            ReadInputHandler(),
            ReadEnvHandler(),
        )
}
