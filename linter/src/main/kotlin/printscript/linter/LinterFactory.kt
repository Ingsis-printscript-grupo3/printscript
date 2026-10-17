package printscript.linter

import printscript.common.ConfigParser
import printscript.linter.rule.LinterRuleRegistry
import java.io.InputStream

object LinterFactory {
    // sin config chequea todo (el CLI sin --config). Una config que no nombra ninguna
    // regla, como el {} del TCK, no chequea nada: cada regla solo corre si su clave esta
    fun create(
        registry: LinterRuleRegistry = LinterRuleRegistry(),
        config: Map<String, String> = registry.defaultConfig(),
    ): Linter = Linter(registry.rulesFor(config))

    // el TCK manda la config como stream, sin extension que mirar
    fun fromStream(input: InputStream): Linter = create(config = ConfigParser.parseStream(input))

    fun fromFile(path: String): Linter = create(config = ConfigParser.parseFile(path))
}
