package printscript.semantic

import printscript.ast.*
import printscript.semantic.symbol.SymbolTable

class ExpressionResolver(private val symbolTable: SymbolTable) {

    fun resolveType(expression: Expression): SemanticResult<String> {
        return when (expression) {
            is NumberLiteral -> SemanticResult.Success("number")
            is StringLiteral -> SemanticResult.Success("string")
            is Identifier -> symbolTable.lookup(expression.name)
            is BinaryExpression -> {
                val leftResult = resolveType(expression.left)
                val rightResult = resolveType(expression.right)
                
                if (leftResult is SemanticResult.Failure) return leftResult
                if (rightResult is SemanticResult.Failure) return rightResult
                
                val left = (leftResult as SemanticResult.Success).value
                val right = (rightResult as SemanticResult.Success).value
                
                if (expression.operator == printscript.common.TokenType.PLUS) {
                    if (left == "number" && right == "number") return SemanticResult.Success("number")
                    return SemanticResult.Success("string")
                }

                if (left == "number" && right == "number") return SemanticResult.Success("number")
                SemanticResult.Failure("Semantic Error: Incompatible types in operation.")
            }
            else -> SemanticResult.Failure("Semantic Error: Unknown expression.")
        }
    }
}
