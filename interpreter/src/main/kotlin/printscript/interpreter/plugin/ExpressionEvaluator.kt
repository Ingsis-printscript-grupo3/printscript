package printscript.interpreter.plugin

import printscript.ast.Expression
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value

// plugin that knows how to evaluate one kind of expression. same pattern as
// StatementInterpreter: the guard lives once here, the concrete type goes in the
// signature.
abstract class ExpressionEvaluator<T : Expression>(val type: Class<T>) {
    fun matches(expression: Expression): Boolean = type.isInstance(expression)

    fun evaluate(
        expression: Expression,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value {
        if (!matches(expression)) throw UnknownExpressionError(expression)
        return doEvaluate(type.cast(expression), env, interpreter)
    }

    protected abstract fun doEvaluate(
        expression: T,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value
}
