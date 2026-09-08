package printscript.semantic.handler.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.common.TokenType
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

class BinaryExpressionHandler : Handler<Expression, ExpressionResolver, SemanticResult<String>> {
    override fun applies(node: Expression) = node is BinaryExpression

    override fun handle(
        node: Expression,
        ctx: ExpressionResolver,
    ): SemanticResult<String> {
        if (node !is BinaryExpression) {
            return SemanticResult.Failure("Semantic Error: Unexpected node in BinaryExpressionHandler.", node.position)
        }

        val leftResult = ctx.resolveType(node.left)
        val rightResult = ctx.resolveType(node.right)

        return when {
            leftResult is SemanticResult.Failure -> leftResult
            rightResult is SemanticResult.Failure -> rightResult
            else -> {
                val left = (leftResult as SemanticResult.Success).value
                val right = (rightResult as SemanticResult.Success).value
                resultFor(node.operator, left, right, node.position)
            }
        }
    }

    private fun resultFor(
        operator: TokenType,
        left: String,
        right: String,
        position: printscript.common.Position = printscript.common.Position(0, 0),
    ): SemanticResult<String> {
        if (left == "boolean" || right == "boolean") {
            return SemanticResult.Failure(
                "Semantic Error: Operator '$operator' cannot be applied to boolean types.",
                position,
            )
        }

        return when {
            operator == TokenType.PLUS && left == "number" && right == "number" -> SemanticResult.Success("number")
            operator == TokenType.PLUS && (left == "string" || right == "string") -> SemanticResult.Success("string")
            operator in listOf(TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE) &&
                left == "number" && right == "number" -> SemanticResult.Success("number")
            else -> SemanticResult.Failure("Semantic Error: Incompatible types in operation.", position)
        }
    }
}
