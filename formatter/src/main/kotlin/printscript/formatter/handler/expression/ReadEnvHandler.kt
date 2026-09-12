package printscript.formatter.handler.expression

import printscript.ast.Expression
import printscript.ast.ReadEnv
import printscript.ast.registry.Handler
import printscript.formatter.Formatter

class ReadEnvHandler : Handler<Expression, Formatter, String> {
    override fun applies(node: Expression) = node is ReadEnv

    override fun handle(
        node: Expression,
        ctx: Formatter,
    ): String {
        check(node is ReadEnv) { "Expected ReadEnv, got $node" }
        return "readEnv(${ctx.formatExpression(node.argument)})"
    }
}
