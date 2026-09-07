package printscript.interpreter

sealed interface Value

data class NumberValue(val value: Double) : Value

data class StringValue(val value: String) : Value

data class BooleanValue(val value: Boolean) : Value

fun Value.textOf(): String =
    when (this) {
        is NumberValue -> if (this.value % 1.0 == 0.0) this.value.toLong().toString() else this.value.toString()
        is StringValue -> this.value
        is BooleanValue -> this.value.toString()
    }

fun Value.typeName(): String =
    when (this) {
        is NumberValue -> "number"
        is StringValue -> "string"
        is BooleanValue -> "boolean"
    }
