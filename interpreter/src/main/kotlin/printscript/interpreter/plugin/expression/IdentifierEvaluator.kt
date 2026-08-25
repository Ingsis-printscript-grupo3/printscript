package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

class IdentifierEvaluator : ExpressionEvaluator {

    override fun matches(expression: Expression) = expression is Identifier

    override fun evaluate(expression: Expression, env: Environment, interpreter: InterpreterInterface): Value {
        if (expression !is Identifier) throw UnknownExpressionError(expression)

        return env.lookup(expression.name)
    }
}
