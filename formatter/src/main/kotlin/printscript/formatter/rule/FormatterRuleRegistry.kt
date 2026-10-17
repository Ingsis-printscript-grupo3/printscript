package printscript.formatter.rule

import printscript.formatter.FormatterRules

// cuantas lineas en blanco puede pedir line-breaks-after-println
private val PRINTLN_BLANK_LINES = 0..2

// el catalogo de reglas: aca se define cada una, una sola vez. Agregar una es crear su clase y sumar
// una linea en DEFAULT_FACTORIES; ni Formatter.kt, ni el loader, ni el validador se tocan.
// El orden de la lista es el orden en que corren: primero las que deciden en que linea va cada token,
// despues las de espacio dentro de la linea y al final la indentacion, que necesita saber cuantas
// lineas quedaron. register() agrega al final, asi que una regla nueva tiene la ultima palabra.
// Devuelve un registry nuevo en vez de mutar, igual que LinterRuleRegistry
class FormatterRuleRegistry(
    val factories: List<FormatterRuleFactory> = DEFAULT_FACTORIES,
    val checks: List<FormatterRuleCheck> = DEFAULT_CHECKS,
) {
    val keys: Set<String> get() = factories.map { it.key }.toSet()

    // dos reglas con la misma key correrian las dos: se rechaza en vez de elegir una en silencio
    fun register(factory: FormatterRuleFactory): FormatterRuleRegistry {
        require(factory.key !in keys) { "formatter key '${factory.key}' is already registered" }
        return FormatterRuleRegistry(factories + factory, checks)
    }

    // la config ya viene validada por FormatterRulesValidator
    fun rulesFor(config: FormatterRules): List<FormatterRule> =
        factories.mapNotNull { factory -> config.values[factory.key]?.let(factory::create) }

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

        // combinaciones que no se pueden pedir juntas
        val DEFAULT_CHECKS: List<FormatterRuleCheck> =
            listOf(
                exclusive("if-brace-same-line", "if-brace-below-line"),
                exclusive("enforce-spacing-around-equals", "enforce-no-spacing-around-equals"),
            )
    }
}
