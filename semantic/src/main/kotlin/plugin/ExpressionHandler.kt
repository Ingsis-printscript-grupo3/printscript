package printscript.semantic.plugin

import printscript.ast.Expression
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.UnknownExpressionError
import printscript.semantic.symbol.SymbolTable

abstract class ExpressionHandler<T : Expression>(val type: Class<T>) {
    fun matches(expression: Expression): Boolean = type.isInstance(expression)

    fun resolveType(
        expression: Expression,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<String> {
        if (!matches(expression)) throw UnknownExpressionError(expression)
        return doResolveType(type.cast(expression), symbolTable, resolver)
    }

    protected abstract fun doResolveType(
        expression: T,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<String>
}
