package printscript.semantic

import printscript.ast.Statement
import printscript.ast.registry.Registry
import printscript.semantic.handler.statement.AssignmentHandler
import printscript.semantic.handler.statement.PrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler
import printscript.semantic.symbol.SymbolTable

class StatementValidator(
    val symbolTable: SymbolTable,
    val expressionResolver: ExpressionResolver,
    private val registry: Registry<Statement, StatementValidator, SemanticResult<Unit>> =
        Registry(
            listOf(
                VariableDeclarationHandler(),
                AssignmentHandler(),
                PrintCallHandler(),
            ),
        ),
) {
    fun validate(statement: Statement): SemanticResult<Unit> =
        registry.resolveOrNull(statement, this)
            ?: SemanticResult.Failure("Semantic Error: Unknown statement type.", statement.position)
}
