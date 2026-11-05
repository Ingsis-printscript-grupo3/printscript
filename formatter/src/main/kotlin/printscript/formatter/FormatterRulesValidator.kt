package printscript.formatter

import printscript.formatter.rule.FormatterRuleRegistry

// la unica validacion de una config: keys desconocidas, tipo y rango de cada valor (lo sabe cada factory)
// y las combinaciones prohibidas que declara el registry. La llaman el loader al leer el archivo y
// FormatterFactory antes de crear las reglas, asi una config armada en codigo tambien falla en vez de
// dejar una regla apagada sin avisar
class FormatterRulesValidator(
    private val registry: FormatterRuleRegistry,
) {
    fun check(config: FormatterRules) {
        val unknown = config.values.keys - registry.keys
        require(unknown.isEmpty()) { "unknown formatter keys: ${unknown.joinToString()}" }
        registry.factories.forEach { factory -> config.values[factory.key]?.let(factory::validate) }
        registry.checks.forEach { it.check(config) }
    }
}
