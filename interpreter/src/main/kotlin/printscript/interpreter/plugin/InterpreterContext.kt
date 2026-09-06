package printscript.interpreter.plugin

import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface

data class InterpreterContext(
    val env: Environment,
    val interpreter: InterpreterInterface,
)
