package printscript.interpreter.plugin
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface

import printscript.ast.Statement

interface StatementInterpreter<T : Statement> {
    fun execute(statement: T, env: Environment, interpreter: InterpreterInterface)
}






