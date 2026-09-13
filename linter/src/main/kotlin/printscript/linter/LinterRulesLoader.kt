package printscript.linter

import java.io.File
import java.io.InputStream

object LinterRulesLoader {
    private val jsonPairRegex = Regex(""""([^"]+)"\s*:\s*("(?:\\.|[^"\\])*"|\[[^\]]*\]|\{[^}]*\}|[^,\s{}]+)""")

    private val knownKeys =
        setOf(
            "identifier_format",
            "mandatory-variable-or-literal-in-println",
            "mandatory-variable-or-literal-in-readInput",
        )

    fun fromJson(json: String): LinterRules = buildRules(parseFlatJson(json))

    fun fromYaml(yaml: String): LinterRules = buildRules(parseFlatYaml(yaml))

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(input: InputStream): LinterRules {
        val text = input.readBytes().decodeToString().removePrefix("\uFEFF")
        return if (text.trimStart().startsWith("{")) fromJson(text) else fromYaml(text)
    }

    fun fromFile(path: String): LinterRules {
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

    // si la clave no esta, la regla no se crea. Si esta pero vacia, vale el default
    private fun bool(
        map: Map<String, String>,
        key: String,
        default: Boolean,
    ): Boolean? {
        if (!map.containsKey(key)) return null
        return when (map[key]?.lowercase()) {
            null, "", "null" -> default
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Expected boolean for '$key'")
        }
    }

    private fun identifierFormat(map: Map<String, String>): String? {
        if (!map.containsKey("identifier_format")) return null
        val raw = map["identifier_format"]
        return if (raw.isNullOrEmpty() || raw == "null") CAMEL_CASE else raw
    }

    private fun buildRules(map: Map<String, String>): LinterRules {
        map.keys.filter { it !in knownKeys }.forEach {
            System.err.println("linter: ignoring unknown key '$it'")
        }
        return LinterRules(
            identifierFormat = identifierFormat(map),
            printCallArgumentsMustBeLiteralOrIdentifier =
                bool(map, "mandatory-variable-or-literal-in-println", true),
            readInputArgumentsMustBeLiteralOrIdentifier =
                bool(map, "mandatory-variable-or-literal-in-readInput", true),
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
