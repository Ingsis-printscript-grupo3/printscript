package printscript.formatter.handler.expression

import printscript.ast.Expression
import printscript.ast.ReadInput
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class ReadInputHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is ReadInput

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is ReadInput) { "Expected ReadInput, got $node" }
        return "readInput(${ctx.formatExpression(node.argument)})"
    }
}
