package printscript.interpreter.plugin.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.plugin.InterpreterContext

class VariableDeclarationInterpreter : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is VariableDeclaration

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is VariableDeclaration) throw UnknownStatementError(node)

        val expression = node.value
        val value = if (expression != null) ctx.interpreter.evaluate(expression) else null
        ctx.env.declare(node.name, value)
    }
}
