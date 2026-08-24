package printscript.interpreter.plugin.statement
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.StatementInterpreter
import printscript.interpreter.textOf

import printscript.ast.PrintCall

class PrintCallInterpreter(private val output: Output) : StatementInterpreter<PrintCall> {
    override fun execute(statement: PrintCall, env: Environment, interpreter: InterpreterInterface) {
        val value = interpreter.evaluate(statement.value)
        output.emit(value.textOf())
    }
}






