package printscript.formatter

import printscript.common.ConfigParser
import printscript.formatter.rule.FormatterRuleRegistry
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

    // cada factory convierte su texto; solo se guardan las reglas pedidas
    private fun buildRules(
        map: Map<String, String>,
        registry: FormatterRuleRegistry,
    ): FormatterRules {
        val values =
            registry.factories
                .mapNotNull { factory -> map[factory.key]?.let(factory::parse)?.let { factory.key to it } }
                .toMap()
        val rules = FormatterRules(values)
        FormatterRulesValidator(registry).check(rules)
        (map.keys - registry.keys).forEach { System.err.println("formatter: ignoring unknown key '$it'") }
        return rules
    }
}
