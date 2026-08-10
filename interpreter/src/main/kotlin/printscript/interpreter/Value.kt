package printscript.interpreter

sealed interface Value

data class NumberValue(val value: Double) : Value

data class StringValue(val value: String) : Value
