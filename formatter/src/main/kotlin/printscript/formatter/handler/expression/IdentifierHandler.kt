package printscript.formatter.handler.expression

import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class IdentifierHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is Identifier

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is Identifier) { "Expected Identifier, got $node" }
        return node.name
    }
}
