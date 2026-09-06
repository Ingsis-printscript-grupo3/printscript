package printscript.interpreter.plugin.statement

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.textOf

class PrintCallInterpreter(private val output: Output) : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is PrintCall

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is PrintCall) throw UnknownStatementError(node)

        val value = ctx.interpreter.evaluate(node.value)
        output.emit(value.textOf())
    }
}
