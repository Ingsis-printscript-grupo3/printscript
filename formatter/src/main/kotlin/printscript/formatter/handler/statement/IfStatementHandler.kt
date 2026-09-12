package printscript.formatter.handler.statement

import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class IfStatementHandler : Handler<Statement, Formatter, String> {
    override fun applies(node: Statement) = node is IfStatement

    override fun handle(
        node: Statement,
        ctx: Formatter,
    ): String {
        check(node is IfStatement) { "Expected IfStatement, got $node" }
        val sameLine = ctx.rules.braceOnSameLine
        val condition = "if (${ctx.formatExpression(node.condition)})"
        val thenBranch = ctx.formatStatement(node.thenBranch)
        val result = StringBuilder()
        result.append(if (sameLine) "$condition $thenBranch" else "$condition\n$thenBranch")
        val elseBranch = node.elseBranch
        if (elseBranch != null) {
            val formatted = ctx.formatStatement(elseBranch)
            result.append(if (sameLine) " else $formatted" else "\nelse\n$formatted")
        }
        return result.toString()
    }
}
