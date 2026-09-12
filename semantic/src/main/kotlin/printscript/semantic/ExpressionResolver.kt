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
    val expectedType: String? = null,
    private val registry: Registry<Expression, ExpressionResolver, SemanticResult<String>> =
        Registry(defaultHandlers()),
) {
    val version: LanguageVersion get() = rules.version

    constructor(
        symbolTable: SymbolTable,
        version: LanguageVersion,
        registry: Registry<Expression, ExpressionResolver, SemanticResult<String>> =
            Registry(defaultHandlers()),
    ) : this(symbolTable, SemanticRules.from(version), null, registry)

    companion object {
        fun defaultHandlers(): List<Handler<Expression, ExpressionResolver, SemanticResult<String>>> =
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

    fun withExpectedType(expectedType: String?): ExpressionResolver =
        if (this.expectedType == expectedType) {
            this
        } else {
            ExpressionResolver(symbolTable, rules, expectedType, registry)
        }

    fun resolveType(
        expression: Expression,
        expectedType: String? = null,
    ): SemanticResult<String> {
        val resolver = withExpectedType(expectedType)
        return resolver.registry.resolveOrNull(expression, resolver)
            ?: SemanticResult.Failure("Semantic Error: Unknown expression type.", expression.position)
    }
}
