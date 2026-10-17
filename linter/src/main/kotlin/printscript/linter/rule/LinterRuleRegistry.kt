package printscript.linter.rule

import printscript.linter.IdentifierFormat

// aca se registra cada regla nueva, asi Linter.kt no se toca mas.
// register() devuelve un registry nuevo en vez de mutar, igual que ast.registry.Registry:
// un registro mutable global se compartiria entre tests y entre instancias de Linter
class LinterRuleRegistry(
    private val factories: List<LinterRuleFactory> = DEFAULT_FACTORIES,
) {
    init {
        // con dos factories para la misma clave, defaultConfig se quedaria con una sola sin avisar
        require(factories.distinctBy { it.key }.size == factories.size) { "Duplicated linter config key" }
    }

    fun register(factory: LinterRuleFactory): LinterRuleRegistry = LinterRuleRegistry(factories + factory)

    // se recorren las factories y no el map, asi el orden de las reglas no depende del archivo
    fun rulesFor(config: Map<String, String>): List<LinterRule> {
        (config.keys - factories.map { it.key }.toSet()).forEach {
            System.err.println("linter: ignoring unknown key '$it'")
        }
        return factories.mapNotNull { it.createFrom(config) }
    }

    // todas las reglas prendidas: lo que usa el CLI cuando no le pasan --config
    fun defaultConfig(): Map<String, String> = factories.associate { it.key to it.default }

    private companion object {
        val DEFAULT_FACTORIES: List<LinterRuleFactory> =
            listOf(
                LinterRuleFactory("identifier_format", default = IdentifierFormat.CAMEL_CASE.configValue) {
                    IdentifierFormatRule(IdentifierFormat.fromConfigValue(it))
                },
                LinterRuleFactory.toggle("mandatory-variable-or-literal-in-println") { PrintCallArgumentRule() },
                LinterRuleFactory.toggle("mandatory-variable-or-literal-in-readInput") { ReadInputArgumentRule() },
            )
    }
}
