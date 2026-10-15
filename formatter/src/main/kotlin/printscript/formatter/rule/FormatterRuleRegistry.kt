package printscript.formatter.rule

import printscript.formatter.FormatterRules

// devuelve lista vacia cuando la config no pide la regla
fun interface FormatterRuleFactory {
    fun create(config: FormatterRules): List<FormatterRule>
}

// aca se registra cada regla nueva, asi Formatter.kt no se toca mas.
// El orden de la lista es el orden en que corren: primero las que deciden en que linea va cada token,
// despues las de espacio dentro de la linea y al final la indentacion, que necesita saber cuantas
// lineas quedaron. register() agrega al final, asi que una regla nueva tiene la ultima palabra.
// Devuelve un registry nuevo en vez de mutar, igual que LinterRuleRegistry
class FormatterRuleRegistry(
    private val factories: List<FormatterRuleFactory> = DEFAULT_FACTORIES,
) {
    fun register(factory: FormatterRuleFactory): FormatterRuleRegistry = FormatterRuleRegistry(factories + factory)

    fun rulesFor(config: FormatterRules): List<FormatterRule> = factories.flatMap { it.create(config) }

    private companion object {
        val DEFAULT_FACTORIES: List<FormatterRuleFactory> =
            listOf(
                // saltos de linea
                rule { config -> config.lineBreaksAfterPrintln?.let { LineBreaksAfterPrintln(it) } },
                rule { config -> LineBreakAfterStatement.takeIf { config.lineBreakAfterStatement } },
                rule { config -> IfBraceSameLine.takeIf { config.ifBraceSameLine } },
                rule { config -> IfBraceBelowLine.takeIf { config.ifBraceBelowLine } },
                // espacios dentro de la linea: single-space va primero para que las demas la pisen
                rule { config -> SingleSpaceSeparation.takeIf { config.singleSpaceSeparation } },
                rule { config -> SpaceBeforeColon.takeIf { config.spaceBeforeColon } },
                rule { config -> SpaceAfterColon.takeIf { config.spaceAfterColon } },
                rule { config -> SpacingAroundEquals.takeIf { config.spacingAroundEquals } },
                rule { config -> NoSpacingAroundEquals.takeIf { config.noSpacingAroundEquals } },
                rule { config -> SpaceSurroundingOperations.takeIf { config.spaceSurroundingOperations } },
                // indentacion
                rule { config -> config.indentInsideIf?.let { IndentInsideIf(it) } },
            )

        // la mayoria de las reglas son una sola, prendida o apagada por config
        private fun rule(create: (FormatterRules) -> FormatterRule?) =
            FormatterRuleFactory { config -> listOfNotNull(create(config)) }
    }
}
