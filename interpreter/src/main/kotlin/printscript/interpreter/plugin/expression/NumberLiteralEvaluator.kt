package printscript.interpreter.plugin.expression

import printscript.ast.NumberLiteral
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.NumberValue
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

class NumberLiteralEvaluator : ExpressionEvaluator<NumberLiteral>(NumberLiteral::class.java) {
    override fun doEvaluate(
        expression: NumberLiteral,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value {
        return NumberValue(expression.value)
    }
}
