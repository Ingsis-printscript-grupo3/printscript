package printscript.interpreter.plugin

import printscript.ast.Statement
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface

// plugin q sabe ejecutar un tipo de statement
// mismo patron q TokenReader en el lexer, primero pregunta si le corresponde y desp ejecuta
interface StatementInterpreter {
    fun matches(statement: Statement): Boolean

    fun execute(
        statement: Statement,
        env: Environment,
        interpreter: InterpreterInterface,
    )
}
