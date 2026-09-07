package printscript.interpreter.plugin.statement

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.ValueConverter
import printscript.interpreter.plugin.InterpreterContext

class Assignment11Interpreter : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is Assignment

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is Assignment) throw UnknownStatementError(node)

        val rawValue = ctx.interpreter.evaluate(node.value)
        val targetType = ctx.env.typeOf(node.name)
        val value =
            if (targetType != null) {
                ValueConverter.convert(rawValue, targetType, node.value, ctx)
            } else {
                rawValue
            }
        ctx.env.assign(node.name, value)
    }
}
