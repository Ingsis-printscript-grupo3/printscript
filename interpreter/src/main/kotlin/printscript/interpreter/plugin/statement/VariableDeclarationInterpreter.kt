package printscript.interpreter.plugin.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.ValueConverter
import printscript.interpreter.plugin.InterpreterContext

class VariableDeclarationInterpreter : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is VariableDeclaration

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is VariableDeclaration) throw UnknownStatementError(node)

        val expression = node.value
        val value =
            if (expression != null) {
                val rawValue = ctx.interpreter.evaluate(expression)
                ValueConverter.convert(rawValue, node.type, at = node.namePosition)
            } else {
                null
            }
        ctx.env.declare(node.name, value, node.type, isConst = node.isConst, at = node.namePosition)
    }
}
