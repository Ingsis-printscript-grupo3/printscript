package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.NumberLiteral
import printscript.ast.registry.Handler
import printscript.interpreter.NumberValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.plugin.InterpreterContext

class NumberLiteralEvaluator : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is NumberLiteral

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is NumberLiteral) throw UnknownExpressionError(node)

        return NumberValue(node.value)
    }
}
