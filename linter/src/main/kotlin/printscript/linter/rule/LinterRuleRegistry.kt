package printscript.linter.rule

import printscript.linter.LinterRules

// devuelve null cuando la config apaga la regla
fun interface LinterRuleFactory {
    fun create(config: LinterRules): LinterRule?
}

// aca se registra cada regla nueva, asi Linter.kt no se toca mas.
// register() devuelve un registry nuevo en vez de mutar, igual que ast.registry.Registry:
// un registro mutable global se compartiria entre tests y entre instancias de Linter
class LinterRuleRegistry(
    private val factories: List<LinterRuleFactory> = DEFAULT_FACTORIES,
) {
    fun register(factory: LinterRuleFactory): LinterRuleRegistry = LinterRuleRegistry(factories + factory)

    fun rulesFor(config: LinterRules): List<LinterRule> = factories.mapNotNull { it.create(config) }

    private companion object {
        val DEFAULT_FACTORIES: List<LinterRuleFactory> =
            listOf(
                LinterRuleFactory { config ->
                    if (config.hasIdentifierFormat) IdentifierFormatRule(config.identifierFormat) else null
                },
                LinterRuleFactory { config ->
                    if (config.hasPrintCallArguments && config.printCallArgumentsMustBeLiteralOrIdentifier) {
                        PrintCallArgumentRule()
                    } else {
                        null
                    }
                },
                LinterRuleFactory { config ->
                    if (config.hasReadInputArguments && config.readInputArgumentsMustBeLiteralOrIdentifier) {
                        ReadInputArgumentRule()
                    } else {
                        null
                    }
                },
            )
    }
}
