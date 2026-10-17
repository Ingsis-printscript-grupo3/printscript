package printscript.semantic

import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.semantic.handler.expression.BinaryExpressionHandler
import printscript.semantic.handler.expression.BooleanLiteralHandler
import printscript.semantic.handler.expression.IdentifierHandler
import printscript.semantic.handler.expression.NumberLiteralHandler
import printscript.semantic.handler.expression.ReadEnvHandler
import printscript.semantic.handler.expression.ReadInputHandler
import printscript.semantic.handler.expression.StringLiteralHandler
import printscript.semantic.symbol.SymbolTable

class ExpressionResolver(
    val symbolTable: SymbolTable,
    val version: LanguageVersion = LanguageVersion.V1_1,
    private val registry: Registry<Expression, ResolverContext, SemanticResult<String>> =
        Registry(defaultHandlers(version)),
) {
    companion object {
        fun defaultHandlers(
            version: LanguageVersion,
        ): List<Handler<Expression, ResolverContext, SemanticResult<String>>> =
            when (version) {
                LanguageVersion.V1_0 -> default10Handlers()
                LanguageVersion.V1_1 -> default11Handlers()
            }

        fun default10Handlers(): List<Handler<Expression, ResolverContext, SemanticResult<String>>> =
            listOf(
                NumberLiteralHandler(),
                StringLiteralHandler(),
                IdentifierHandler(),
                BinaryExpressionHandler(),
            )

        fun default11Handlers(): List<Handler<Expression, ResolverContext, SemanticResult<String>>> =
            default10Handlers() +
                listOf(
                    BooleanLiteralHandler(),
                    ReadInputHandler(),
                    ReadEnvHandler(),
                )
    }

    fun resolveType(
        expression: Expression,
        expectedType: String? = null,
    ): SemanticResult<String> =
        registry.resolveOrNull(expression, ResolverContext(this, expectedType))
            ?: SemanticResult.Failure("Unknown expression type.", expression.position)
}
