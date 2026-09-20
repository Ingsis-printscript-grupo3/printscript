package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.registry.Handler
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.InterpreterContext

class IdentifierEvaluator : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is Identifier

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is Identifier) throw UnknownExpressionError(node)

        return ctx.env.lookup(node.name, at = node.position)
    }
}
