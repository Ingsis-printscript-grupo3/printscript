package printscript.formatter

import java.io.File
import java.io.InputStream

object FormatterRulesLoader {
    private val jsonPairRegex = Regex(""""([^"]+)"\s*:\s*("(?:\\.|[^"\\])*"|\[[^\]]*\]|\{[^}]*\}|[^,\s{}]+)""")

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

    fun fromJson(json: String): FormatterRules = buildRules(parseFlatJson(json))

    fun fromYaml(yaml: String): FormatterRules = buildRules(parseFlatYaml(yaml))

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(input: InputStream): FormatterRules {
        val text = input.readBytes().decodeToString().removePrefix("\uFEFF")
        return if (text.trimStart().startsWith("{")) fromJson(text) else fromYaml(text)
    }

    fun fromFile(path: String): FormatterRules {
        val file = File(path)
        require(file.isFile) { "Config file not found: $path" }
        val text = file.readText().removePrefix("\uFEFF")
        return when (val extension = file.extension.lowercase()) {
            "json" -> fromJson(text)
            "yaml", "yml" -> fromYaml(text)
            else -> throw IllegalArgumentException(
                "Unsupported config file extension '$extension': expected json, yaml or yml",
            )
        }
    }

    private fun parseFlatJson(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        jsonPairRegex.findAll(json.removePrefix("\uFEFF")).forEach { match ->
            val key = match.groupValues[1]
            val rawValue = match.groupValues[2]
            result[key] = unquote(rawValue)
        }
        return result
    }

    private fun parseFlatYaml(yaml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        yaml.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val key = unquote(line.substring(0, colonIdx))
                    val rawValue = line.substring(colonIdx + 1)
                    result[key] = unquote(rawValue)
                }
            }
        }
        return result
    }

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
            System.err.println("formatter: ignoro la clave desconocida '$it'")
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

private fun unquote(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith('"') || trimmed.startsWith('\'')) {
        return extractQuoted(trimmed)
    }
    val commentIdx = trimmed.indexOf('#')
    return if (commentIdx != -1) trimmed.substring(0, commentIdx).trim() else trimmed
}

private fun extractQuoted(trimmed: String): String {
    val quote = trimmed[0]
    val endQuote = findEndQuote(trimmed, quote)
    val content = if (endQuote != -1) trimmed.substring(1, endQuote) else trimmed.removePrefix("$quote")
    return content.replace("\\\"", "\"").replace("\\\\", "\\")
}

private fun findEndQuote(
    text: String,
    quote: Char,
): Int {
    var escaped = false
    for (i in 1 until text.length) {
        val c = text[i]
        if (escaped) {
            escaped = false
        } else if (c == '\\') {
            escaped = true
        } else if (c == quote) {
            return i
        }
    }
    return -1
}
