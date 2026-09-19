package printscript.interpreter.plugin.expression

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.common.TokenType
import printscript.interpreter.NumberValue
import printscript.interpreter.StringValue
import printscript.interpreter.TypeMismatchError
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.textOf
import printscript.interpreter.typeName

class BinaryExpressionEvaluator : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is BinaryExpression

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is BinaryExpression) throw UnknownExpressionError(node)

        return applyOperator(
            node,
            ctx.interpreter.evaluate(node.left),
            ctx.interpreter.evaluate(node.right),
        )
    }

    private fun applyOperator(
        expression: BinaryExpression,
        left: Value,
        right: Value,
    ): Value {
        val operator = expression.operator

        if (operator == TokenType.PLUS) {
            return addOrConcatenate(left, right)
        }

        if (left !is NumberValue || right !is NumberValue) {
            throw TypeMismatchError(left.typeName(), right.typeName(), expression.position)
        }

        return when (operator) {
            TokenType.MINUS -> NumberValue(left.value - right.value)
            TokenType.MULTIPLY -> NumberValue(left.value * right.value)
            TokenType.DIVIDE -> NumberValue(left.value / right.value)
            else -> throw UnknownExpressionError(expression)
        }
    }

    private fun addOrConcatenate(
        left: Value,
        right: Value,
    ): Value =
        if (left is NumberValue && right is NumberValue) {
            NumberValue(left.value + right.value)
        } else {
            StringValue(left.textOf() + right.textOf())
        }
}
