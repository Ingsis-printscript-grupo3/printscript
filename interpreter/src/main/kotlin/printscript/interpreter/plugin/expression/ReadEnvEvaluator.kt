package printscript.interpreter.plugin.expression

import printscript.ast.Expression
import printscript.ast.ReadEnv
import printscript.ast.registry.Handler
import printscript.interpreter.EnvVariableNotFoundError
import printscript.interpreter.StringValue
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.Value
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.textOf

class ReadEnvEvaluator(
    private val envProvider: EnvProvider,
) : Handler<Expression, InterpreterContext, Value> {
    override fun applies(node: Expression) = node is ReadEnv

    override fun handle(
        node: Expression,
        ctx: InterpreterContext,
    ): Value {
        if (node !is ReadEnv) throw UnknownExpressionError(node)

        val varNameValue = ctx.interpreter.evaluate(node.argument)
        val varName = varNameValue.textOf()
        val value = envProvider.getEnv(varName) ?: throw EnvVariableNotFoundError(varName)
        return StringValue(value)
    }
}
