package printscript.semantic.plugin

import printscript.ast.Statement
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.UnknownStatementError
import printscript.semantic.symbol.SymbolTable

// plugin that knows how to validate one kind of Statement. same pattern as
// StatementInterpreter in the interpreter module: the "is this mine" guard lives
// once here (matches + cast), so concrete handlers only implement the typed
// validation.
abstract class StatementHandler<T : Statement>(val type: Class<T>) {
    fun matches(statement: Statement): Boolean = type.isInstance(statement)

    fun validate(
        statement: Statement,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<Unit> {
        if (!matches(statement)) throw UnknownStatementError(statement)
        return doValidate(type.cast(statement), symbolTable, resolver)
    }

    protected abstract fun doValidate(
        statement: T,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<Unit>
}
