package printscript.interpreter.plugin.expression
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.NumberValue
import printscript.interpreter.StringValue
import printscript.interpreter.TypeMismatchError
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator
import printscript.interpreter.textOf
import printscript.interpreter.typeName

class BinaryExpressionEvaluator : ExpressionEvaluator {

    override fun matches(expression: Expression) = expression is BinaryExpression

    override fun evaluate(expression: Expression, env: Environment, interpreter: InterpreterInterface): Value {
        if (expression !is BinaryExpression) throw UnknownExpressionError(expression)

        return applyOperator(
            expression,
            interpreter.evaluate(expression.left),
            interpreter.evaluate(expression.right),
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
            throw TypeMismatchError(left.typeName(), right.typeName())
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
