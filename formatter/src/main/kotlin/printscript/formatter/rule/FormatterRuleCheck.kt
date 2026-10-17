package printscript.formatter.rule

import printscript.formatter.FormatterRules

// una restriccion entre reglas que no le pertenece a ninguna sola: falla si la config la rompe
fun interface FormatterRuleCheck {
    fun check(config: FormatterRules)
}

// las dos reglas se contradicen: pedir las dos a la vez es un error
internal fun exclusive(
    first: String,
    second: String,
) = FormatterRuleCheck { config ->
    require(!(config.values[first] == true && config.values[second] == true)) {
        "$first and $second cannot both be true"
    }
}
