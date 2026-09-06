package printscript.formatter.handler.statement

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class PrintCallHandler : Handler<Statement, Formatter, String> {
    override fun applies(node: Statement) = node is PrintCall

    override fun handle(
        node: Statement,
        ctx: Formatter,
    ): String {
        check(node is PrintCall) { "Expected PrintCall, got $node" }

        return "println(${ctx.formatExpression(node.value)});"
    }
}
