package printscript.formatter.handler.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.common.TokenType
import printscript.formatter.Formatter

class BinaryExpressionHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is BinaryExpression

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is BinaryExpression) { "Expected BinaryExpression, got $node" }

        val left = formatOperand(node.left, node.operator, isRightOperand = false, ctx)
        val right = formatOperand(node.right, node.operator, isRightOperand = true, ctx)
        return "$left ${formatOperator(node.operator)} $right"
    }

    private fun formatOperand(
        operand: Expression,
        parentOperator: TokenType,
        isRightOperand: Boolean,
        ctx: Formatter,
    ): String {
        val text = ctx.formatExpression(operand)
        val needsParentheses =
            operand is BinaryExpression &&
                needsParentheses(operand.operator, parentOperator, isRightOperand)
        return if (needsParentheses) "($text)" else text
    }

    private fun needsParentheses(
        childOperator: TokenType,
        parentOperator: TokenType,
        isRightOperand: Boolean,
    ): Boolean {
        val childPrecedence = precedenceOf(childOperator)
        val parentPrecedence = precedenceOf(parentOperator)
        if (childPrecedence < parentPrecedence) return true
        val parentIsNotAssociative = parentOperator == TokenType.MINUS || parentOperator == TokenType.DIVIDE
        return isRightOperand && childPrecedence == parentPrecedence && parentIsNotAssociative
    }

    private fun precedenceOf(operator: TokenType): Int =
        when (operator) {
            TokenType.MULTIPLY, TokenType.DIVIDE -> 2
            else -> 1
        }

    private fun formatOperator(operator: TokenType): String =
        when (operator) {
            TokenType.PLUS -> "+"
            TokenType.MINUS -> "-"
            TokenType.MULTIPLY -> "*"
            TokenType.DIVIDE -> "/"
            else -> error("Token '$operator' is not a valid binary operator")
        }
}
