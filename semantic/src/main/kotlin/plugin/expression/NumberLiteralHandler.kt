package printscript.semantic.plugin.expression

import printscript.ast.NumberLiteral
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.ExpressionHandler
import printscript.semantic.symbol.SymbolTable

class NumberLiteralHandler : ExpressionHandler<NumberLiteral>(NumberLiteral::class.java) {
    override fun doResolveType(
        expression: NumberLiteral,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<String> = SemanticResult.Success("number")
}
