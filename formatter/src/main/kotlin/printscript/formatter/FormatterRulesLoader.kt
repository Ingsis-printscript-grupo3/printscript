package printscript.formatter

import printscript.common.ConfigParser
import java.io.InputStream

object FormatterRulesLoader {
    fun fromJson(json: String): FormatterRules = buildRules(ConfigParser.parseJson(json))

    fun fromYaml(yaml: String): FormatterRules = buildRules(ConfigParser.parseYaml(yaml))

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(input: InputStream): FormatterRules = buildRules(ConfigParser.parseStream(input))

    fun fromFile(path: String): FormatterRules = buildRules(ConfigParser.parseFile(path))

    // cada key se nombra una sola vez, aca: las que no se leyeron son las desconocidas
    private fun buildRules(map: Map<String, String>): FormatterRules {
        val config = ConfigReader(map)
        val rules =
            FormatterRules(
                spaceBeforeColon = config.bool("enforce-spacing-before-colon-in-declaration"),
                spaceAfterColon = config.bool("enforce-spacing-after-colon-in-declaration"),
                spacingAroundEquals = config.bool("enforce-spacing-around-equals"),
                noSpacingAroundEquals = config.bool("enforce-no-spacing-around-equals"),
                lineBreaksAfterPrintln = config.int("line-breaks-after-println"),
                singleSpaceSeparation = config.bool("mandatory-single-space-separation"),
                spaceSurroundingOperations = config.bool("mandatory-space-surrounding-operations"),
                lineBreakAfterStatement = config.bool("mandatory-line-break-after-statement"),
                indentInsideIf = config.int("indent-inside-if"),
                ifBraceSameLine = config.bool("if-brace-same-line"),
                ifBraceBelowLine = config.bool("if-brace-below-line"),
            )
        config.unreadKeys().forEach { System.err.println("formatter: ignoring unknown key '$it'") }
        return rules
    }
}

// lee la config y se acuerda que keys pidio
private class ConfigReader(
    private val map: Map<String, String>,
) {
    private val read = mutableSetOf<String>()

    fun bool(key: String): Boolean =
        when (raw(key)?.lowercase()) {
            null, "", "null" -> false
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Expected boolean for '$key'")
        }

    fun int(key: String): Int? {
        val raw = raw(key) ?: return null
        if (raw.isEmpty() || raw == "null") return null
        return raw.toIntOrNull() ?: throw IllegalArgumentException("Expected integer for '$key', got '$raw'")
    }

    fun unreadKeys(): Set<String> = map.keys - read

    private fun raw(key: String): String? {
        read += key
        return map[key]
    }
}
