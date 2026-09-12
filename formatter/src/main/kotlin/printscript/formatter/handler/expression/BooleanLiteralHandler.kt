package printscript.formatter.handler.expression

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class BooleanLiteralHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is BooleanLiteral

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is BooleanLiteral) { "Expected BooleanLiteral, got $node" }
        return node.value.toString()
    }
}
