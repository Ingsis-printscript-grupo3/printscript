package printscript.formatter

import printscript.formatter.rule.FormatterRuleRegistry

// la config la lee FormatterRulesLoader: aca solo se elige que reglas van
object FormatterFactory {
    fun create(
        config: FormatterRules = FormatterRules(),
        registry: FormatterRuleRegistry = FormatterRuleRegistry(),
    ): FormatterInterface = Formatter(registry.rulesFor(config))
}
