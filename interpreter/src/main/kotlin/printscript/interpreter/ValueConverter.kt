package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
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

        if (sourceExpr is ReadInput) {
            val text = value.textOf()
            return when (targetType) {
                "string" -> StringValue(text)
                "number" ->
                    text.toDoubleOrNull()?.let { NumberValue(it) }
                        ?: throw ReadInputConversionError(text, targetType)
                "boolean" ->
                    text.toBooleanStrictOrNull()?.let { BooleanValue(it) }
                        ?: throw ReadInputConversionError(text, targetType)
                else -> throw ReadInputConversionError(text, targetType)
            }
        }

        if (sourceExpr is ReadEnv) {
            val text = value.textOf()
            val varName =
                if (ctx != null) {
                    ctx.interpreter.evaluate(sourceExpr.argument).textOf()
                } else {
                    sourceExpr.argument.toString()
                }
            return when (targetType) {
                "string" -> StringValue(text)
                "number" ->
                    text.toDoubleOrNull()?.let { NumberValue(it) }
                        ?: throw ReadEnvConversionError(varName, text, targetType)
                "boolean" ->
                    text.toBooleanStrictOrNull()?.let { BooleanValue(it) }
                        ?: throw ReadEnvConversionError(varName, text, targetType)
                else -> throw ReadEnvConversionError(varName, text, targetType)
            }
        }

        throw TypeMismatchError(value.typeName(), targetType)
    }
}
