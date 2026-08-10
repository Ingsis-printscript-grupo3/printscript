package printscript.semantic

import printscript.ast.*
import printscript.semantic.symbol.SymbolTable

class ExpressionResolver(private val symbolTable: SymbolTable) {

    fun resolveType(expression: Expression): String {
        return when (expression) {
            is NumberLiteral -> "number"
            is StringLiteral -> "string"
            is Identifier -> symbolTable.lookup(expression.name)
            is BinaryExpression -> {
                val left = resolveType(expression.left)
                val right = resolveType(expression.right)
                if (left == "number" && right == "number") return "number"
                throw RuntimeException("Semantic Error: Incompatible types in operation.")
            }
            else -> throw RuntimeException("Semantic Error: Unknown expression.")
        }
    }
}
