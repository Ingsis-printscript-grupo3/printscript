package printscript.semantic

import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.semantic.handler.statement.AssignmentHandler
import printscript.semantic.handler.statement.BlockHandler
import printscript.semantic.handler.statement.IfStatementHandler
import printscript.semantic.handler.statement.PrintCallHandler
import printscript.semantic.handler.statement.VariableDeclarationHandler
import printscript.semantic.symbol.SymbolTable

class StatementValidator(
    val symbolTable: SymbolTable,
    val expressionResolver: ExpressionResolver,
    val rules: SemanticRules,
    private val registry: Registry<Statement, StatementValidator, SemanticResult<Unit>> =
        Registry(defaultHandlers(rules.version)),
) {
    val version: LanguageVersion get() = rules.version

    constructor(
        symbolTable: SymbolTable,
        expressionResolver: ExpressionResolver,
        version: LanguageVersion,
        registry: Registry<Statement, StatementValidator, SemanticResult<Unit>> =
            Registry(defaultHandlers(version)),
    ) : this(symbolTable, expressionResolver, SemanticRules.from(version), registry)

    companion object {
        fun defaultHandlers(
            version: LanguageVersion,
        ): List<Handler<Statement, StatementValidator, SemanticResult<Unit>>> =
            when (version) {
                LanguageVersion.V1_0 -> default10Handlers()
                LanguageVersion.V1_1 -> default11Handlers()
            }

        fun default10Handlers(): List<Handler<Statement, StatementValidator, SemanticResult<Unit>>> =
            listOf(
                VariableDeclarationHandler(),
                AssignmentHandler(),
                PrintCallHandler(),
            )

        fun default11Handlers(): List<Handler<Statement, StatementValidator, SemanticResult<Unit>>> =
            default10Handlers() +
                listOf(
                    IfStatementHandler(),
                    BlockHandler(),
                )
    }

    fun validate(statement: Statement): SemanticResult<Unit> =
        registry.resolveOrNull(statement, this)
            ?: SemanticResult.Failure("Unknown statement type.", statement.position)
}
