package printscript.formatter.handler.statement

import printscript.ast.Assignment
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class AssignmentHandler : Handler<Statement, Formatter, String> {
    override fun applies(node: Statement) = node is Assignment

    override fun handle(
        node: Statement,
        ctx: Formatter,
    ): String {
        check(node is Assignment) { "Expected Assignment, got $node" }

        return "${node.name}${ctx.assignmentOperator()}${ctx.formatExpression(node.value)};"
    }
}
