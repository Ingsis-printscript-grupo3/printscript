package printscript.interpreter.plugin.expression
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.Value
import printscript.interpreter.StringValue
import printscript.interpreter.plugin.ExpressionEvaluator

import printscript.ast.StringLiteral

class StringLiteralEvaluator : ExpressionEvaluator<StringLiteral> {
    override fun evaluate(expression: StringLiteral, env: Environment, interpreter: InterpreterInterface): Value {
        return StringValue(expression.value)
    }
}






