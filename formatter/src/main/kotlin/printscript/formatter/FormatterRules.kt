package printscript.formatter

// los valores de la config ya leidos y validados, por key. Que significa cada key lo dice FormatterRuleRegistry.
// Si una regla no viene en la config, ese espacio queda como estaba en el archivo
data class FormatterRules(
    val values: Map<String, Any> = emptyMap(),
) {
    companion object {
        fun of(vararg entries: Pair<String, Any>) = FormatterRules(mapOf(*entries))
    }
}
