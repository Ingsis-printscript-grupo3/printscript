package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.ReadInput
import printscript.ast.registry.Handler
import printscript.interpreter.StringValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.input.InputProvider
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.textOf

class ReadInputEvaluator(
    private val inputProvider: InputProvider,
) : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is ReadInput

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is ReadInput) throw UnknownExpressionError(node)

        val promptValue = ctx.interpreter.evaluate(node.argument)
        val input = inputProvider.readInput(promptValue.textOf())
        return StringValue(input)
    }
}
