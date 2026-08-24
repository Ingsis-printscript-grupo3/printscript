package printscript.interpreter.plugin
import printscript.ast.Expression
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.Value

interface ExpressionEvaluator<T : Expression> {
    fun evaluate(
        expression: T,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value
}
