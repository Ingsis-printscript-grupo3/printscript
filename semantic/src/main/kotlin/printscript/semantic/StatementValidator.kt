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
        Registry(defaultHandlers()),
) {
    val version: LanguageVersion get() = rules.version

    constructor(
        symbolTable: SymbolTable,
        expressionResolver: ExpressionResolver,
        version: LanguageVersion,
        registry: Registry<Statement, StatementValidator, SemanticResult<Unit>> =
            Registry(defaultHandlers()),
    ) : this(symbolTable, expressionResolver, SemanticRules.from(version), registry)

    companion object {
        fun defaultHandlers(): List<Handler<Statement, StatementValidator, SemanticResult<Unit>>> =
            listOf(
                VariableDeclarationHandler(),
                AssignmentHandler(),
                PrintCallHandler(),
                IfStatementHandler(),
                BlockHandler(),
            )
    }

    fun validate(statement: Statement): SemanticResult<Unit> =
        registry.resolveOrNull(statement, this)
            ?: SemanticResult.Failure("Semantic Error: Unknown statement type.", statement.position)
}
