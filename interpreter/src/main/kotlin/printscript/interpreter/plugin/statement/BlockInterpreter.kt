package printscript.interpreter.plugin.statement

import printscript.ast.Block
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.plugin.InterpreterContext

class BlockInterpreter : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is Block

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is Block) throw UnknownStatementError(node)

        ctx.env.enterScope()
        try {
            ctx.interpreter.interpret(node.statements.iterator())
        } finally {
            ctx.env.exitScope()
        }
    }
}
