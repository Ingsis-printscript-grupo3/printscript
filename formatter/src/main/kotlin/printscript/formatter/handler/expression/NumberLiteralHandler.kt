package printscript.formatter.handler.expression

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class NumberLiteralHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is NumberLiteral

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is NumberLiteral) { "Expected NumberLiteral, got $node" }
        return formatNumber(node.value)
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
