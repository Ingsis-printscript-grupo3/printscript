package printscript.linter.rule

// una entrada por clave de la config: que regla crea y con que valor si la clave viene vacia.
// La regla no sabe nada de la config, asi que leer la clave es trabajo de su factory
class LinterRuleFactory(
    val key: String,
    val default: String,
    // devuelve null cuando el valor apaga la regla
    private val build: (value: String) -> LinterRule?,
) {
    // clave ausente: la regla no va (el {} del TCK). Vacia o "null": vale el default
    fun createFrom(config: Map<String, String>): LinterRule? {
        val value = config[key] ?: return null
        val isBlank = value.isEmpty() || value.equals("null", ignoreCase = true)
        return build(if (isBlank) default else value)
    }

    companion object {
        // la mayoria de las reglas solo se prenden o se apagan
        fun toggle(
            key: String,
            rule: () -> LinterRule,
        ): LinterRuleFactory = LinterRuleFactory(key, default = "true") { if (parseBoolean(key, it)) rule() else null }
    }
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
