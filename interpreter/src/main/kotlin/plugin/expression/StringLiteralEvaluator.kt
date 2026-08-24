package printscript.interpreter.plugin.expression
import printscript.ast.StringLiteral
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.StringValue
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

class StringLiteralEvaluator : ExpressionEvaluator<StringLiteral> {
    override fun evaluate(
        expression: StringLiteral,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value {
        return StringValue(expression.value)
    }
}
