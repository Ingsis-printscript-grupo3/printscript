package printscript.interpreter.plugin.expression

import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.registry.Handler
import printscript.interpreter.BooleanValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.InterpreterContext

class BooleanLiteralEvaluator : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is BooleanLiteral

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is BooleanLiteral) throw UnknownExpressionError(node)

        return BooleanValue(node.value)
    }
}
