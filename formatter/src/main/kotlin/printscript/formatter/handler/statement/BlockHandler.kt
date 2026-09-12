package printscript.formatter.handler.statement

import printscript.ast.Block
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class BlockHandler : Handler<Statement, Formatter, String> {
    override fun applies(node: Statement) = node is Block

    override fun handle(
        node: Statement,
        ctx: Formatter,
    ): String {
        check(node is Block) { "Expected Block, got $node" }
        val indent = " ".repeat(ctx.rules.indentInsideIf)
        // agrega la sangria a todas las lineas p los if anidados
        val body =
            node.statements.joinToString("\n") { statement ->
                ctx.formatStatement(statement).lines().joinToString("\n") { indent + it }
            }
        return if (body.isEmpty()) "{\n}" else "{\n$body\n}"
    }
}
