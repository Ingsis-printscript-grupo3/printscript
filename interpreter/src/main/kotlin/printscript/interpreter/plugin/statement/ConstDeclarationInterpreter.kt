package printscript.interpreter.plugin.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.ValueConverter
import printscript.interpreter.plugin.InterpreterContext

class ConstDeclarationInterpreter : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is VariableDeclaration && node.isConst

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is VariableDeclaration || !node.isConst) throw UnknownStatementError(node)

        val expression = node.value
        val rawValue = if (expression != null) ctx.interpreter.evaluate(expression) else null
        val value =
            if (rawValue != null) {
                ValueConverter.convert(rawValue, node.type, expression, ctx)
            } else {
                null
            }
        ctx.env.declare(node.name, value, node.type, isConst = true)
    }
}
