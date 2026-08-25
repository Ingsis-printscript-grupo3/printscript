package printscript.interpreter.plugin.statement

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.StatementInterpreter
import printscript.interpreter.textOf

class PrintCallInterpreter(private val output: Output) : StatementInterpreter {

    override fun matches(statement: Statement) = statement is PrintCall

    override fun execute(statement: Statement, env: Environment, interpreter: InterpreterInterface) {
        if (statement !is PrintCall) throw UnknownStatementError(statement)

        val value = interpreter.evaluate(statement.value)
        output.emit(value.textOf())
    }
}
