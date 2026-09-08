package printscript.semantic

import printscript.ast.Expression
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.BooleanLiteralHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.symbol.SymbolTable

class ExpressionResolver(
    val symbolTable: SymbolTable,
    val version: LanguageVersion,
    private val registry: Registry<Expression, ExpressionResolver, SemanticResult<String>> =
        Registry(
            listOf(
                NumberLiteralHandler(),
                StringLiteralHandler(),
                BooleanLiteralHandler(),
                IdentifierHandler(),
                BinaryExpressionHandler(),
            ),
        ),
) {
    fun resolveType(expression: Expression): SemanticResult<String> =
        registry.resolveOrNull(expression, this)
            ?: SemanticResult.Failure("Semantic Error: Unknown expression type.", expression.position)
}
