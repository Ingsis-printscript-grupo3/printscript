package printscript.formatter.handler.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class VariableDeclarationHandler : Handler<Statement, Formatter, String> {
    override fun applies(node: Statement) = node is VariableDeclaration

    override fun handle(
        node: Statement,
        ctx: Formatter,
    ): String {
        check(node is VariableDeclaration) { "Expected VariableDeclaration, got $node" }

        val rules = ctx.rules
        val colon = "${if (rules.spaceBeforeColon) " " else ""}:${if (rules.spaceAfterColon) " " else ""}"
        val keyword = if (node.isConst) "const" else "let"
        val declaration = "$keyword ${node.name}$colon${node.type}"
        return node.value?.let { "$declaration${ctx.assignmentOperator()}${ctx.formatExpression(it)};" }
            ?: "$declaration;"
    }
}
