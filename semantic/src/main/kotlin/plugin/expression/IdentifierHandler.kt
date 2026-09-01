package printscript.semantic.plugin.expression

import printscript.ast.Identifier
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.ExpressionHandler
import printscript.semantic.symbol.SymbolTable

class IdentifierHandler : ExpressionHandler<Identifier>(Identifier::class.java) {
    override fun doResolveType(
        expression: Identifier,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<String> = symbolTable.lookup(expression.name)
}
