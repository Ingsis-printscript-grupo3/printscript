package printscript.semantic

import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.semantic.symbol.SymbolTable

typealias SemanticStatementHandler = Handler<Statement, StatementValidator, SemanticResult<Unit>>

class StatementValidator(
    val symbolTable: SymbolTable,
    val expressionResolver: ExpressionResolver,
    handlers: List<SemanticStatementHandler>,
) {
    private val registry = Registry(handlers)

    fun validate(statement: Statement): SemanticResult<Unit> =
        registry.resolveOrNull(statement, this)
            ?: SemanticResult.Failure("Unknown statement type.", statement.position)
}
