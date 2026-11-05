package printscript.formatter

import printscript.formatter.rule.FormatterRuleRegistry

// la config la lee FormatterRulesLoader: aca se valida y se eligen que reglas van
object FormatterFactory {
    fun create(
        config: FormatterRules = FormatterRules(),
        registry: FormatterRuleRegistry = FormatterRuleRegistry(),
    ): Formatter {
        FormatterRulesValidator(registry).check(config)
        return Formatter(registry.rulesFor(config))
    }
}
