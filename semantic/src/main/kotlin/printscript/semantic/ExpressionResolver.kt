package printscript.semantic

import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.semantic.symbol.SymbolTable

typealias SemanticExpressionHandler = Handler<Expression, ResolverContext, SemanticResult<String>>

class ExpressionResolver(
    val symbolTable: SymbolTable,
    handlers: List<SemanticExpressionHandler>,
) {
    private val registry = Registry(handlers)

    fun resolveType(
        expression: Expression,
        expectedType: String? = null,
    ): SemanticResult<String> =
        registry.resolveOrNull(expression, ResolverContext(this, expectedType))
            ?: SemanticResult.Failure("Unknown expression type.", expression.position)
}
