package printscript.linter.rule

import printscript.linter.LinterRules

// devuelve null cuando la config apaga la regla
fun interface LinterRuleFactory {
    fun create(config: LinterRules): LinterRule?
}

// aca se registra cada regla nueva, asi Linter.kt no se toca mas
object LinterRuleRegistry {
    private val factories: List<LinterRuleFactory> =
        listOf(
            LinterRuleFactory { config -> IdentifierFormatRule(config.identifierFormat) },
            LinterRuleFactory { config ->
                if (config.printCallArgumentsMustBeLiteralOrIdentifier) PrintCallArgumentRule() else null
            },
            LinterRuleFactory { config ->
                if (config.readInputArgumentsMustBeLiteralOrIdentifier) ReadInputArgumentRule() else null
            },
        )

    fun rulesFor(config: LinterRules): List<LinterRule> = factories.mapNotNull { it.create(config) }
}
