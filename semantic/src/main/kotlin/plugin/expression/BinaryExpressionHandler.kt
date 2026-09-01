package printscript.semantic.plugin.expression

import printscript.ast.BinaryExpression
import printscript.common.TokenType
import printscript.semantic.ExpressionResolverInterface
import printscript.semantic.SemanticResult
import printscript.semantic.plugin.ExpressionHandler
import printscript.semantic.symbol.SymbolTable

class BinaryExpressionHandler : ExpressionHandler<BinaryExpression>(BinaryExpression::class.java) {
    override fun doResolveType(
        expression: BinaryExpression,
        symbolTable: SymbolTable,
        resolver: ExpressionResolverInterface,
    ): SemanticResult<String> {
        // recursion via the back-reference, same as plugin.evaluate(expr, env, this) in interpreter
        val leftResult = resolver.resolveType(expression.left)
        val rightResult = resolver.resolveType(expression.right)

        if (leftResult is SemanticResult.Failure) return leftResult
        if (rightResult is SemanticResult.Failure) return rightResult

        val left = (leftResult as SemanticResult.Success).value
        val right = (rightResult as SemanticResult.Success).value

        if (expression.operator == TokenType.PLUS) {
            if (left == "number" && right == "number") return SemanticResult.Success("number")
            return SemanticResult.Success("string")
        }

        if (left == "number" && right == "number") return SemanticResult.Success("number")
        return SemanticResult.Failure("Semantic Error: Incompatible types in operation.")
    }
}
