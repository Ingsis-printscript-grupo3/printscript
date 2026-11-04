package printscript.semantic

import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.semantic.symbol.SymbolTable

class StatementValidator(
    val symbolTable: SymbolTable,
    val expressionResolver: ExpressionResolver,
    handlers: List<Handler<Statement, StatementValidator, SemanticResult<Unit>>>,
) {
    private val registry = Registry(handlers)

    fun validate(statement: Statement): SemanticResult<Unit> =
        registry.resolveOrNull(statement, this)
            ?: SemanticResult.Failure("Unknown statement type.", statement.position)
}
