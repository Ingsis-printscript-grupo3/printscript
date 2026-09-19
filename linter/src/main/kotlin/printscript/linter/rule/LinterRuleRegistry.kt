package printscript.linter.rule

import printscript.linter.LinterRules

// devuelve lista vacia cuando la config no pide la regla
fun interface LinterRuleFactory {
    fun create(config: LinterRules): List<LinterRule>
}

// aca se registra cada regla nueva, asi Linter.kt no se toca mas.
// register() devuelve un registry nuevo en vez de mutar, igual que ast.registry.Registry:
// un registro mutable global se compartiria entre tests y entre instancias de Linter
class LinterRuleRegistry(
    private val factories: List<LinterRuleFactory> = DEFAULT_FACTORIES,
) {
    fun register(factory: LinterRuleFactory): LinterRuleRegistry = LinterRuleRegistry(factories + factory)

    fun rulesFor(config: LinterRules): List<LinterRule> = factories.flatMap { it.create(config) }

    // el Linter recorre una sola regla: las que pidio la config vienen agrupadas
    fun ruleFor(config: LinterRules): LinterRule = CompositeRule(rulesFor(config))

    private companion object {
        val DEFAULT_FACTORIES: List<LinterRuleFactory> =
            listOf(
                LinterRuleFactory { config ->
                    val format = config.identifierFormat
                    if (format != null) listOf(IdentifierFormatRule(format)) else emptyList()
                },
                // println y readInput piden lo mismo, asi que van juntas en un grupo
                LinterRuleFactory { config -> argumentRules(config) },
            )

        private fun argumentRules(config: LinterRules): List<LinterRule> {
            val rules =
                buildList {
                    if (config.printCallArgumentsMustBeLiteralOrIdentifier == true) {
                        add(PrintCallArgumentRule())
                    }
                    if (config.readInputArgumentsMustBeLiteralOrIdentifier == true) {
                        add(ReadInputArgumentRule())
                    }
                }
            return if (rules.isEmpty()) emptyList() else listOf(CompositeRule(rules))
        }
    }
}
