package printscript.interpreter.plugin
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

import printscript.ast.Expression

interface ExpressionEvaluator<T : Expression> {
    fun evaluate(expression: T, env: Environment, interpreter: InterpreterInterface): Value
}






