package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.StringLiteral
import printscript.ast.registry.Handler
import printscript.interpreter.StringValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.InterpreterContext

class StringLiteralEvaluator : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is StringLiteral

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is StringLiteral) throw UnknownExpressionError(node)

        return StringValue(node.value)
    }
}
