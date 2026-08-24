package printscript.interpreter.plugin.statement
import printscript.ast.PrintCall
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.plugin.StatementInterpreter
import printscript.interpreter.textOf

class PrintCallInterpreter(private val output: (String) -> Unit) : StatementInterpreter<PrintCall> {
    override fun execute(
        statement: PrintCall,
        env: Environment,
        interpreter: InterpreterInterface,
    ) {
        val value = interpreter.evaluate(statement.value)
        output(value.textOf())
    }
}
