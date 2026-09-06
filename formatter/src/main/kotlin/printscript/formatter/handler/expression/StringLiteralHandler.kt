package printscript.formatter.handler.expression

import printscript.ast.Expression
import printscript.ast.StringLiteral
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class StringLiteralHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is StringLiteral

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is StringLiteral) { "Expected StringLiteral, got $node" }
        return "\"${node.value}\""
    }
}
