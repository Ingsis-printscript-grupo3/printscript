package printscript.semantic.plugin.expression

import printscript.ast.StringLiteral
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.ExpressionHandler
import printscript.semantic.symbol.SymbolTable

class StringLiteralHandler : ExpressionHandler<StringLiteral>(StringLiteral::class.java) {
    override fun doResolveType(
        expression: StringLiteral,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<String> = SemanticResult.Success("string")
}
