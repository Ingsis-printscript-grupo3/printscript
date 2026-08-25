package printscript.interpreter.plugin.statement
import printscript.ast.Assignment
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.plugin.StatementInterpreter

class AssignmentInterpreter : StatementInterpreter<Assignment> {
    override fun execute(
        statement: Assignment,
        env: Environment,
        interpreter: InterpreterInterface,
    ) {
        env.assign(statement.name, interpreter.evaluate(statement.value))
    }
}
