package printscript.interpreter.plugin

import printscript.ast.Expression
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.Value

//plugin q sabe evaluar un tipo de expresion
interface ExpressionEvaluator {

    fun matches(expression: Expression): Boolean

    fun evaluate(expression: Expression, env: Environment, interpreter: InterpreterInterface): Value
}
