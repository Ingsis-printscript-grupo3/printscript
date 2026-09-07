package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.interpreter.plugin.InterpreterContext

object ValueConverter {
    fun convert(
        value: Value,
        targetType: String,
        sourceExpr: Expression? = null,
        ctx: InterpreterContext? = null,
    ): Value {
        if (value.typeName() == targetType) {
            return value
        }

        return when (sourceExpr) {
            is ReadInput -> convertReadInput(value.textOf(), targetType)
            is ReadEnv -> convertReadEnv(value.textOf(), targetType, sourceExpr, ctx)
            else -> throw TypeMismatchError(value.typeName(), targetType)
        }
    }

    private fun convertReadInput(
        text: String,
        targetType: String,
    ): Value {
        val converted = tryConvert(text, targetType)
        return converted ?: throw ReadInputConversionError(text, targetType)
    }

    private fun convertReadEnv(
        text: String,
        targetType: String,
        sourceExpr: ReadEnv,
        ctx: InterpreterContext?,
    ): Value {
        val varName = resolveEnvName(sourceExpr, ctx)
        val converted = tryConvert(text, targetType)
        return converted ?: throw ReadEnvConversionError(varName, text, targetType)
    }

    private fun tryConvert(
        text: String,
        targetType: String,
    ): Value? =
        when (targetType) {
            "string" -> StringValue(text)
            "number" -> text.toDoubleOrNull()?.let { NumberValue(it) }
            "boolean" -> text.toBooleanStrictOrNull()?.let { BooleanValue(it) }
            else -> null
        }

    private fun resolveEnvName(
        sourceExpr: ReadEnv,
        ctx: InterpreterContext?,
    ): String {
        val arg = sourceExpr.argument
        return when {
            ctx != null -> ctx.interpreter.evaluate(arg).textOf()
            arg is StringLiteral -> arg.value
            else -> arg.toString()
        }
    }
}
