package printscript.semantic.handler.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.common.Position
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
            return SemanticResult.Failure("Unexpected node in BinaryExpressionHandler.", node.position)
        }

        val left =
            when (val leftResult = ctx.resolveType(node.left)) {
                is SemanticResult.Failure -> return leftResult
                is SemanticResult.Success -> leftResult.value
            }

        val right =
            when (val rightResult = ctx.resolveType(node.right)) {
                is SemanticResult.Failure -> return rightResult
                is SemanticResult.Success -> rightResult.value
            }

        return resultFor(node.operator, left, right, node.position)
    }

    private fun resultFor(
        operator: TokenType,
        left: String,
        right: String,
        position: Position,
    ): SemanticResult<String> {
        if (left == "boolean" || right == "boolean") {
            return SemanticResult.Failure(
                "Operator '$operator' cannot be applied to boolean types.",
                position,
            )
        }

        return when {
            operator == TokenType.PLUS && left == "number" && right == "number" -> SemanticResult.Success("number")
            operator == TokenType.PLUS && (left == "string" || right == "string") -> SemanticResult.Success("string")
            operator in listOf(TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE) &&
                left == "number" && right == "number" -> SemanticResult.Success("number")
            else -> SemanticResult.Failure("Incompatible types in operation.", position)
        }
    }
}
