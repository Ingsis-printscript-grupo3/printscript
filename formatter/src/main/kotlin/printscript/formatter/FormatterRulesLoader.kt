package printscript.formatter

import printscript.common.ConfigParser
import printscript.formatter.rule.BoolRuleFactory
import printscript.formatter.rule.FormatterRuleRegistry
import printscript.formatter.rule.IntRuleFactory
import java.io.InputStream

// no conoce ninguna key: lee las que declara el registry. Si se registra una regla de afuera,
// hay que pasar ese mismo registry para que su key no se tome como desconocida
object FormatterRulesLoader {
    fun fromJson(
        json: String,
        registry: FormatterRuleRegistry = FormatterRuleRegistry(),
    ): FormatterRules = buildRules(ConfigParser.parseJson(json), registry)

    fun fromYaml(
        yaml: String,
        registry: FormatterRuleRegistry = FormatterRuleRegistry(),
    ): FormatterRules = buildRules(ConfigParser.parseYaml(yaml), registry)

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(
        input: InputStream,
        registry: FormatterRuleRegistry = FormatterRuleRegistry(),
    ): FormatterRules = buildRules(ConfigParser.parseStream(input), registry)

    fun fromFile(
        path: String,
        registry: FormatterRuleRegistry = FormatterRuleRegistry(),
    ): FormatterRules = buildRules(ConfigParser.parseFile(path), registry)

    // solo se guardan las reglas pedidas: un bool en false o un valor vacio es como no mencionarla
    private fun buildRules(
        map: Map<String, String>,
        registry: FormatterRuleRegistry,
    ): FormatterRules {
        val config = ConfigReader(map)
        val values =
            registry.factories
                .mapNotNull { factory ->
                    val value: Any? =
                        when (factory) {
                            is BoolRuleFactory -> config.bool(factory.key).takeIf { it }
                            is IntRuleFactory -> config.int(factory.key, factory.range)
                        }
                    value?.let { factory.key to it }
                }.toMap()
        val rules = FormatterRules(values)
        registry.check(rules)
        config.unreadKeys().forEach { System.err.println("formatter: ignoring unknown key '$it'") }
        return rules
    }
}

// lee la config y se acuerda que keys pidio
private class ConfigReader(
    private val map: Map<String, String>,
) {
    private val read = mutableSetOf<String>()

    fun bool(key: String): Boolean =
        when (readValue(key)?.lowercase()) {
            null, "", "null" -> false
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Expected boolean for '$key'")
        }

    fun int(
        key: String,
        range: IntRange,
    ): Int? {
        val value = readValue(key) ?: return null
        if (value.isEmpty() || value == "null") return null
        val number =
            value.toIntOrNull() ?: throw IllegalArgumentException("Expected integer for '$key', got '$value'")
        require(number in range) { "$key must be ${range.first}..${range.last}, was $number" }
        return number
    }

    fun unreadKeys(): Set<String> = map.keys - read

    // devuelve el valor de la key como texto, sin convertir, y la marca como leida
    private fun readValue(key: String): String? {
        read += key
        return map[key]
    }
}
