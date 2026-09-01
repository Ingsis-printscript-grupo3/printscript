package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.semantic.plugin.DefaultStatementHandlers
import printscript.semantic.plugin.StatementHandler
import printscript.semantic.symbol.SymbolTable

// dispatches over a list of handlers via matches(), same as interpreter (see ExpressionResolver)
class StatementValidator(
    private val symbolTable: SymbolTable,
    private val expressionResolver: ExpressionResolverInterface,
    private val handlers: List<StatementHandler<*>> = DefaultStatementHandlers.list,
) {
    fun validate(statement: Statement): SemanticResult<Unit> {
        statementExhaustivenessWitness(statement)

        val handler =
            handlers.firstOrNull { it.matches(statement) }
                ?: throw UnknownStatementError(statement)

        return handler.validate(statement, symbolTable, expressionResolver)
    }

    // compile-time safety net: see the comment in ExpressionResolver.
    // what to do when it fails: add the new branch here AND register its StatementHandler
    // in DefaultStatementHandlers.
    private fun statementExhaustivenessWitness(statement: Statement) {
        when (statement) {
            is VariableDeclaration -> {}
            is Assignment -> {}
            is PrintCall -> {}
        }
    }
}
