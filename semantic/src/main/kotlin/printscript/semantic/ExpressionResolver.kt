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
    val rules: SemanticRules,
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
    val version: LanguageVersion get() = rules.version

    constructor(
        symbolTable: SymbolTable,
        version: LanguageVersion,
        registry: Registry<Expression, ExpressionResolver, SemanticResult<String>> =
            Registry(
                listOf(
                    NumberLiteralHandler(),
                    StringLiteralHandler(),
                    BooleanLiteralHandler(),
                    IdentifierHandler(),
                    BinaryExpressionHandler(),
                ),
            ),
    ) : this(symbolTable, SemanticRules.from(version), registry)

    fun resolveType(expression: Expression): SemanticResult<String> =
        registry.resolveOrNull(expression, this)
            ?: SemanticResult.Failure("Semantic Error: Unknown expression type.", expression.position)
}
