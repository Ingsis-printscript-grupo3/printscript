package printscript.semantic

import printscript.semantic.symbol.SymbolTable

// lo que recibe cada handler de expresiones: el resolver para sus subexpresiones y el tipo que espera quien pregunta
data class ResolverContext(
    val resolver: ExpressionResolver,
    val expectedType: String?,
) {
    val symbolTable: SymbolTable get() = resolver.symbolTable
}
