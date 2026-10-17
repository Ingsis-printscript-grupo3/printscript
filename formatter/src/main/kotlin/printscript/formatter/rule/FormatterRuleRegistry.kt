package printscript.formatter.rule

import printscript.formatter.FormatterRules

// cuantas lineas en blanco puede pedir line-breaks-after-println
private val PRINTLN_BLANK_LINES = 0..2

// la receta de una regla: que key de la config la prende, de que tipo es y como se crea con el valor.
// Son solo datos: la config la lee FormatterRulesLoader recorriendo esta lista
sealed interface FormatterRuleFactory {
    val key: String
}

class BoolRuleFactory(
    override val key: String,
    val create: () -> FormatterRule,
) : FormatterRuleFactory

class IntRuleFactory(
    override val key: String,
    val range: IntRange,
    val create: (Int) -> FormatterRule,
) : FormatterRuleFactory

// una restriccion entre reglas que no le pertenece a ninguna sola: falla si la config la rompe
fun interface FormatterRuleCheck {
    fun check(config: FormatterRules)
}

// aca se define cada regla, una sola vez: agregar una es crear su clase y sumar una linea en DEFAULT_FACTORIES.
// Ni Formatter.kt ni el loader se tocan.
// El orden de la lista es el orden en que corren: primero las que deciden en que linea va cada token,
// despues las de espacio dentro de la linea y al final la indentacion, que necesita saber cuantas
// lineas quedaron. register() agrega al final, asi que una regla nueva tiene la ultima palabra.
// Devuelve un registry nuevo en vez de mutar, igual que LinterRuleRegistry
class FormatterRuleRegistry(
    val factories: List<FormatterRuleFactory> = DEFAULT_FACTORIES,
    private val checks: List<FormatterRuleCheck> = DEFAULT_CHECKS,
) {
    fun register(factory: FormatterRuleFactory): FormatterRuleRegistry =
        FormatterRuleRegistry(factories + factory, checks)

    fun check(config: FormatterRules) = checks.forEach { it.check(config) }

    // toda config pasa por aca, venga del loader o armada en codigo: una key mal escrita
    // falla en vez de dejar la regla apagada sin avisar
    fun rulesFor(config: FormatterRules): List<FormatterRule> {
        val unknown = config.values.keys - factories.map { it.key }.toSet()
        require(unknown.isEmpty()) { "unknown formatter keys: ${unknown.joinToString()}" }
        check(config)
        return factories.mapNotNull { factory ->
            val value = config.values[factory.key]
            when (factory) {
                is BoolRuleFactory -> factory.create().takeIf { value == true }
                is IntRuleFactory -> (value as? Int)?.let { createInRange(factory, it) }
            }
        }
    }

    private fun createInRange(
        factory: IntRuleFactory,
        value: Int,
    ): FormatterRule {
        val range = factory.range
        require(value in range) { "${factory.key} must be ${range.first}..${range.last}, was $value" }
        return factory.create(value)
    }

    private companion object {
        val DEFAULT_FACTORIES: List<FormatterRuleFactory> =
            listOf(
                // saltos de linea
                IntRuleFactory("line-breaks-after-println", PRINTLN_BLANK_LINES) { LineBreaksAfterPrintln(it) },
                BoolRuleFactory("mandatory-line-break-after-statement") { LineBreakAfterStatement },
                BoolRuleFactory("if-brace-same-line") { IfBraceSameLine },
                BoolRuleFactory("if-brace-below-line") { IfBraceBelowLine },
                // espacios dentro de la linea: single-space va primero para que las demas la pisen
                BoolRuleFactory("mandatory-single-space-separation") { SingleSpaceSeparation },
                BoolRuleFactory("enforce-spacing-before-colon-in-declaration") { SpaceBeforeColon },
                BoolRuleFactory("enforce-spacing-after-colon-in-declaration") { SpaceAfterColon },
                BoolRuleFactory("enforce-spacing-around-equals") { SpacingAroundEquals },
                BoolRuleFactory("enforce-no-spacing-around-equals") { NoSpacingAroundEquals },
                BoolRuleFactory("mandatory-space-surrounding-operations") { SpaceSurroundingOperations },
                // indentacion
                IntRuleFactory("indent-inside-if", 0..Int.MAX_VALUE) { IndentInsideIf(it) },
            )

        val DEFAULT_CHECKS: List<FormatterRuleCheck> =
            listOf(
                exclusive("if-brace-same-line", "if-brace-below-line"),
            )

        private fun exclusive(
            first: String,
            second: String,
        ) = FormatterRuleCheck { config ->
            require(!(config.values[first] == true && config.values[second] == true)) {
                "$first and $second cannot both be true"
            }
        }
    }
}
