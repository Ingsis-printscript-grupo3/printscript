package printscript.formatter.rule

import printscript.formatter.Gap

// varias reglas que se usan como si fueran una, solo cuando se cumple la condicion
internal class RuleGroup(
    private val rules: List<FormatterRule>,
    private val appliesTo: (Gap) -> Boolean = { true },
) : FormatterRule {
    override fun apply(gap: Gap) {
        if (appliesTo(gap)) rules.forEach { it.apply(gap) }
    }
}
