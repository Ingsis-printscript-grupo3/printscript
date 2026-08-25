package printscript.interpreter.plugin.statement

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.plugin.StatementInterpreter

class AssignmentInterpreter : StatementInterpreter {

    override fun matches(statement: Statement) = statement is Assignment

    override fun execute(statement: Statement, env: Environment, interpreter: InterpreterInterface) {
        if (statement !is Assignment) throw UnknownStatementError(statement)

        env.assign(statement.name, interpreter.evaluate(statement.value))
    }
}
