package printscript.interpreter

import printscript.common.Position

object ValueConverter {
    fun convert(
        value: Value,
        targetType: String,
        at: Position,
    ): Value {
        if (value.typeName() == targetType) {
            return value
        }

        val converted =
            when (targetType) {
                "string" -> StringValue(value.textOf())
                "number" -> value.textOf().toDoubleOrNull()?.let { NumberValue(it) }
                "boolean" -> value.textOf().toBooleanStrictOrNull()?.let { BooleanValue(it) }
                else -> null
            }

        return converted ?: throw ValueConversionError(value, targetType, at)
    }
}
