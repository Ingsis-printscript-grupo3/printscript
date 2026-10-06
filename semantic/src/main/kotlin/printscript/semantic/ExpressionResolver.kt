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
    val rules: SemanticRules,
    private val registry: Registry<Expression, ExpressionResolver, SemanticResult<String>>,
    val expectedType: String? = null,
) {
    companion object {
        fun defaultHandlers(
            version: LanguageVersion,
        ): List<Handler<Expression, ExpressionResolver, SemanticResult<String>>> =
            when (version) {
                LanguageVersion.V1_0 -> default10Handlers()
                LanguageVersion.V1_1 -> default11Handlers()
            }

        fun default10Handlers(): List<Handler<Expression, ExpressionResolver, SemanticResult<String>>> =
            listOf(
                NumberLiteralHandler(),
                StringLiteralHandler(),
                IdentifierHandler(),
                BinaryExpressionHandler(),
            )

        fun default11Handlers(): List<Handler<Expression, ExpressionResolver, SemanticResult<String>>> =
            default10Handlers() +
                listOf(
                    BooleanLiteralHandler(),
                    ReadInputHandler(),
                    ReadEnvHandler(),
                )
    }

    fun withExpectedType(expectedType: String?): ExpressionResolver =
        if (this.expectedType == expectedType) {
            this
        } else {
            ExpressionResolver(symbolTable, rules, registry, expectedType)
        }

    fun resolveType(
        expression: Expression,
        expectedType: String? = null,
    ): SemanticResult<String> {
        val resolver = withExpectedType(expectedType)
        return resolver.registry.resolveOrNull(expression, resolver)
            ?: SemanticResult.Failure("Unknown expression type.", expression.position)
    }
}
