package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.NumberValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

class NumberLiteralEvaluator : ExpressionEvaluator {
    override fun matches(expression: Expression) = expression is NumberLiteral

    override fun evaluate(
        expression: Expression,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value {
        if (expression !is NumberLiteral) throw UnknownExpressionError(expression)

        return NumberValue(expression.value)
    }
}
