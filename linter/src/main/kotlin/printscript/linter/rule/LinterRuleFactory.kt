package printscript.linter.rule

import printscript.linter.IdentifierFormat

// una entrada por clave de la config: que regla crea y con que valor si la clave viene vacia.
// La regla no sabe nada de la config, asi que leer la clave es trabajo de su factory
interface LinterRuleFactory {
    val key: String
    val default: String

    // clave ausente: la regla no va (el {} del TCK). Vacia o "null": vale el default
    fun createFrom(config: Map<String, String>): LinterRule?
}

// Factoria polimorfica para reglas que se encienden o apagan con un valor booleano
class BooleanRuleFactory(
    override val key: String,
    override val default: String = "true",
    private val ruleSupplier: () -> LinterRule,
) : LinterRuleFactory {
    constructor(key: String, rule: LinterRule) : this(key, "true", { rule })
    constructor(key: String, default: String, rule: LinterRule) : this(key, default, { rule })

    override fun createFrom(config: Map<String, String>): LinterRule? {
        val value = config[key] ?: return null
        val isBlank = value.isEmpty() || value.equals("null", ignoreCase = true)
        val effective = if (isBlank) default else value
        return if (parseBoolean(key, effective)) ruleSupplier() else null
    }
}

// Factoria polimorfica para la regla de formato de identificadores (camelCase / snake_case)
class IdentifierFormatRuleFactory(
    override val key: String = "identifier_format",
    override val default: String = IdentifierFormat.CAMEL_CASE.configValue,
) : LinterRuleFactory {
    override fun createFrom(config: Map<String, String>): LinterRule? {
        val value = config[key] ?: return null
        val isBlank = value.isEmpty() || value.equals("null", ignoreCase = true)
        val effective = if (isBlank) default else value
        return IdentifierFormatRule(IdentifierFormat.fromConfigValue(effective))
    }
}

internal fun parseBoolean(
    key: String,
    value: String,
): Boolean =
    when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException("Expected boolean for '$key'")
    }
