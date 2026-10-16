package printscript.linter

import printscript.common.LanguageVersion
import printscript.linter.rule.LinterRuleRegistry
import java.io.InputStream

object LinterFactory {
    fun create(
        config: LinterRules = LinterRules(),
        registry: LinterRuleRegistry = defaultRegistry(),
    ): LinterInterface =
        Linter(
            rules = registry.rulesFor(config),
        )

    fun create(
        version: LanguageVersion,
        config: LinterRules = LinterRules(),
    ): LinterInterface =
        when (version) {
            LanguageVersion.V1_0 -> create10(config)
            LanguageVersion.V1_1 -> create11(config)
        }

    fun create10(config: LinterRules = LinterRules()): LinterInterface =
        // en 1.0 el parser rechaza readInput antes de armar el AST, asi que esa regla no
        // puede disparar nunca: se apaga por config en vez de duplicar las factories del registry
        create(
            config = config.copy(readInputArgumentsMustBeLiteralOrIdentifier = false),
        )

    fun create11(config: LinterRules = LinterRules()): LinterInterface = create(config = config)

    // la config llega de afuera (TCK, CLI, runner) y cada formato ya lo resuelve
    // LinterRulesLoader: la fabrica solo evita que cada llamador encadene las dos cosas
    fun fromJson(
        json: String,
        version: LanguageVersion,
    ): LinterInterface = create(version, LinterRulesLoader.fromJson(json))

    fun fromYaml(
        yaml: String,
        version: LanguageVersion,
    ): LinterInterface = create(version, LinterRulesLoader.fromYaml(yaml))

    fun fromStream(
        input: InputStream,
        version: LanguageVersion,
    ): LinterInterface = create(version, LinterRulesLoader.fromStream(input))

    fun fromFile(
        path: String,
        version: LanguageVersion,
    ): LinterInterface = create(version, LinterRulesLoader.fromFile(path))

    fun defaultRegistry(): LinterRuleRegistry = LinterRuleRegistry()
}
