package printscript.linter.rule

import printscript.linter.LinterRules

// devuelve null cuando la config no pide la regla
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

    // el Linter recorre una sola regla: las que pidio la config vienen agrupadas
    fun ruleFor(config: LinterRules): LinterRule = CompositeRule(rulesFor(config))

    private companion object {
        val DEFAULT_FACTORIES: List<LinterRuleFactory> =
            listOf(
                LinterRuleFactory { config ->
                    config.identifierFormat?.let { IdentifierFormatRule(it) }
                },
                // println y readInput piden lo mismo, asi que van juntas en un grupo
                LinterRuleFactory { config -> argumentRules(config) },
            )

        private fun argumentRules(config: LinterRules): LinterRule? {
            val rules =
                listOfNotNull(
                    PrintCallArgumentRule().takeIf { config.printCallArgumentsMustBeLiteralOrIdentifier == true },
                    ReadInputArgumentRule().takeIf { config.readInputArgumentsMustBeLiteralOrIdentifier == true },
                )
            return if (rules.isEmpty()) null else CompositeRule(rules)
        }
    }
}
