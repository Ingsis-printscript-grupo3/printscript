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
        val body = StringBuilder()
        node.statements.forEachIndexed { index, statement ->
            if (index > 0) body.append("\n")
            // agrega la sangria a todas las lineas p los if anidados, menos a las vacias
            val lines = ctx.formatStatement(statement).lines()
            body.append(lines.joinToString("\n") { if (it.isEmpty()) it else indent + it })
            if (index < node.statements.lastIndex) body.append(ctx.lineBreaksAfter(statement))
        }
        return if (body.isEmpty()) "{\n}" else "{\n$body\n}"
    }
}
