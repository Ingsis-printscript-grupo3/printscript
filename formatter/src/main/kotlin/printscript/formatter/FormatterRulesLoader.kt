package printscript.formatter

import printscript.common.ConfigParser
import java.io.InputStream

object FormatterRulesLoader {
    private val knownKeys =
        setOf(
            "enforce-spacing-before-colon-in-declaration",
            "enforce-spacing-after-colon-in-declaration",
            "enforce-spacing-around-equals",
            "enforce-no-spacing-around-equals",
            "line-breaks-after-println",
            "mandatory-single-space-separation",
            "mandatory-space-surrounding-operations",
            "mandatory-line-break-after-statement",
            "indent-inside-if",
            "if-brace-same-line",
            "if-brace-below-line",
        )

    fun fromJson(json: String): FormatterRules = buildRules(ConfigParser.parseJson(json))

    fun fromYaml(yaml: String): FormatterRules = buildRules(ConfigParser.parseYaml(yaml))

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(input: InputStream): FormatterRules = buildRules(ConfigParser.parseStream(input))

    fun fromFile(path: String): FormatterRules = buildRules(ConfigParser.parseFile(path))

    private fun bool(
        map: Map<String, String>,
        key: String,
    ): Boolean =
        when (map[key]?.lowercase()) {
            null, "", "null" -> false
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Expected boolean for '$key'")
        }

    private fun int(
        map: Map<String, String>,
        key: String,
    ): Int? {
        val raw = map[key] ?: return null
        if (raw.isEmpty() || raw == "null") return null
        return raw.toIntOrNull() ?: throw IllegalArgumentException("Expected integer for '$key', got '$raw'")
    }

    private fun buildRules(map: Map<String, String>): FormatterRules {
        map.keys.filter { it !in knownKeys }.forEach {
            System.err.println("formatter: ignoring unknown key '$it'")
        }
        return FormatterRules(
            spaceBeforeColon = bool(map, "enforce-spacing-before-colon-in-declaration"),
            spaceAfterColon = bool(map, "enforce-spacing-after-colon-in-declaration"),
            spacingAroundEquals = bool(map, "enforce-spacing-around-equals"),
            noSpacingAroundEquals = bool(map, "enforce-no-spacing-around-equals"),
            lineBreaksAfterPrintln = int(map, "line-breaks-after-println"),
            singleSpaceSeparation = bool(map, "mandatory-single-space-separation"),
            spaceSurroundingOperations = bool(map, "mandatory-space-surrounding-operations"),
            lineBreakAfterStatement = bool(map, "mandatory-line-break-after-statement"),
            indentInsideIf = int(map, "indent-inside-if"),
            ifBraceSameLine = bool(map, "if-brace-same-line"),
            ifBraceBelowLine = bool(map, "if-brace-below-line"),
        )
    }
}
