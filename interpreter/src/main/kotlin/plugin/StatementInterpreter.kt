package printscript.interpreter.plugin
import printscript.ast.Statement
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface

interface StatementInterpreter<T : Statement> {
    fun execute(
        statement: T,
        env: Environment,
        interpreter: InterpreterInterface,
    )
}
