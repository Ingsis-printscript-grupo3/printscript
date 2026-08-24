package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.StringLiteral
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.StringValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

class StringLiteralEvaluator : ExpressionEvaluator {

    override fun matches(expression: Expression) = expression is StringLiteral

    override fun evaluate(expression: Expression, env: Environment, interpreter: InterpreterInterface): Value {
        if (expression !is StringLiteral) throw UnknownExpressionError(expression)

        return StringValue(expression.value)
    }
}
