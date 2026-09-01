package printscript.interpreter.plugin.statement

import printscript.ast.PrintCall
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.StatementInterpreter
import printscript.interpreter.textOf

class PrintCallInterpreter(private val output: Output) : StatementInterpreter<PrintCall>(PrintCall::class.java) {
    override fun doExecute(
        statement: PrintCall,
        env: Environment,
        interpreter: InterpreterInterface,
    ) {
        val value = interpreter.evaluate(statement.value)
        output.emit(value.textOf())
    }
}
