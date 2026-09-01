package printscript.semantic

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral
import printscript.semantic.plugin.DefaultExpressionHandlers
import printscript.semantic.plugin.ExpressionHandler
import printscript.semantic.symbol.SymbolTable

// dispatches over a list of handlers via matches(), same as interpreter. it's recursive
// (BinaryExpression resolves both of its sides), so it passes itself as a back-reference
// to each handler, same as plugin.evaluate(expression, environment, this) in Interpreter.
class ExpressionResolver(
    private val symbolTable: SymbolTable,
    private val handlers: List<ExpressionHandler<*>> = DefaultExpressionHandlers.list,
) : ExpressionResolverInterface {
    override fun resolveType(expression: Expression): SemanticResult<String> {
        expressionExhaustivenessWitness(expression)

        val handler =
            handlers.firstOrNull { it.matches(expression) }
                ?: throw UnknownExpressionError(expression)

        return handler.resolveType(expression, symbolTable, this)
    }

    // compile-time safety net: a private exhaustive `when` with no `else`. it doesn't
    // decide anything (every branch is empty): its sole purpose is to make the compiler
    // stop compiling this module when a new Expression subtype is added to the sealed
    // interface in ast.
    // what to do when it fails: add the new branch here AND register its ExpressionHandler
    // in DefaultExpressionHandlers (the completeness test will also fail if you forget
    // to register it).
    private fun expressionExhaustivenessWitness(expression: Expression) {
        when (expression) {
            is NumberLiteral -> {}
            is StringLiteral -> {}
            is Identifier -> {}
            is BinaryExpression -> {}
        }
    }
}
