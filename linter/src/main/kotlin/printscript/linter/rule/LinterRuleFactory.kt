package printscript.linter.rule

import printscript.linter.IdentifierFormat

// una entrada por clave de la config: que regla crea y con que valor si la clave viene vacia.
// leer el mapa es trabajo del registry, asi la factory recibe el valor ya resuelto y
// ninguna puede tratar distinto una clave vacia
interface LinterRuleFactory {
    val key: String
    val default: String

    // null = el valor apaga la regla
    fun create(value: String): LinterRule?
}

object IdentifierFormatRuleFactory : LinterRuleFactory {
    override val key = "identifier_format"
    override val default = IdentifierFormat.CAMEL_CASE.configValue

    override fun create(value: String): LinterRule = IdentifierFormatRule(IdentifierFormat.fromConfigValue(value))
}

object PrintCallArgumentRuleFactory : LinterRuleFactory {
    override val key = "mandatory-variable-or-literal-in-println"
    override val default = "true"

    override fun create(value: String): LinterRule? = if (parseBoolean(key, value)) PrintCallArgumentRule() else null
}

object ReadInputArgumentRuleFactory : LinterRuleFactory {
    override val key = "mandatory-variable-or-literal-in-readInput"
    override val default = "true"

    override fun create(value: String): LinterRule? = if (parseBoolean(key, value)) ReadInputArgumentRule() else null
}

private fun parseBoolean(
    key: String,
    value: String,
): Boolean =
    when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException("Expected boolean for '$key'")
    }
