package printscript.interpreter.plugin.expression
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.Value
import printscript.interpreter.NumberValue
import printscript.interpreter.plugin.ExpressionEvaluator

import printscript.ast.NumberLiteral

class NumberLiteralEvaluator : ExpressionEvaluator<NumberLiteral> {
    override fun evaluate(expression: NumberLiteral, env: Environment, interpreter: InterpreterInterface): Value {
        return NumberValue(expression.value)
    }
}






