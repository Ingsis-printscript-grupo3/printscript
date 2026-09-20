package printscript.common

import java.io.File
import java.io.InputStream

object ConfigParser {
    fun parseJson(json: String): Map<String, String> = JsonScanner(json.removePrefix("\uFEFF")).scan()

    fun parseYaml(yaml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        yaml.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val isQuoted = line.startsWith('"') || line.startsWith('\'')
                val endQuote = if (isQuoted) findEndQuote(line, line[0]) else -1
                val startColon = if (endQuote != -1) endQuote + 1 else 0
                val colonIdx = line.indexOf(':', startColon)
                if (colonIdx != -1) {
                    val key = unquote(line.substring(0, colonIdx))
                    val rawValue = line.substring(colonIdx + 1)
                    result[key] = unquote(rawValue)
                }
            }
        }
        return result
    }

    fun parseStream(input: InputStream): Map<String, String> {
        val text = input.readBytes().decodeToString().removePrefix("\uFEFF")
        return if (text.trimStart().startsWith("{")) parseJson(text) else parseYaml(text)
    }

    fun parseFile(path: String): Map<String, String> = parseFile(File(path))

    fun parseFile(file: File): Map<String, String> {
        require(file.isFile) { "Config file not found: ${file.path}" }
        val text = file.readText().removePrefix("\uFEFF")
        return when (val extension = file.extension.lowercase()) {
            "json" -> parseJson(text)
            "yaml", "yml" -> parseYaml(text)
            else -> throw IllegalArgumentException(
                "Unsupported config file extension '$extension': expected json, yaml or yml",
            )
        }
    }
}

private class JsonScanner(private val text: String) {
    private val n = text.length
    private var i = 0

    fun scan(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        i = skipToContent(0)
        while (i < n && text[i] != '}') {
            if (text[i] == ',') {
                i++
                continue
            }
            if (text[i] == '"' || text[i] == '\'') {
                val (key, afterKey) = readQuoted(i)
                val afterColon = skipColon(afterKey)
                val (value, afterVal) = readValue(afterColon)
                result[unquote(key)] = unquote(value)
                i = afterVal
            } else {
                i++
            }
        }
        return result
    }

    private fun skipToContent(start: Int): Int {
        var idx = skipWhitespace(start)
        if (idx < n && text[idx] == '{') idx++
        return idx
    }

    private fun skipWhitespace(start: Int): Int {
        var idx = start
        while (idx < n && text[idx].isWhitespace()) idx++
        return idx
    }

    private fun skipColon(start: Int): Int {
        var idx = skipWhitespace(start)
        if (idx < n && text[idx] == ':') idx++
        return skipWhitespace(idx)
    }

    private fun readQuoted(start: Int): Pair<String, Int> {
        val quote = text[start]
        var idx = start + 1
        var escaped = false
        while (idx < n) {
            val c = text[idx]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == quote) {
                idx++
                break
            }
            idx++
        }
        return Pair(text.substring(start, idx), idx)
    }

    private fun readValue(start: Int): Pair<String, Int> {
        val idx = skipWhitespace(start)
        if (idx >= n) return Pair("", idx)
        val c = text[idx]
        return when {
            c == '"' || c == '\'' -> readQuoted(idx)
            c == '{' || c == '[' -> readCompound(idx)
            else -> readPrimitive(idx)
        }
    }

    private fun readCompound(start: Int): Pair<String, Int> {
        val open = text[start]
        val close = if (open == '{') '}' else ']'
        var depth = 0
        var idx = start
        while (idx < n) {
            val c = text[idx]
            if (c == '"' || c == '\'') {
                idx = readQuoted(idx).second
                continue
            }
            if (c == open) depth++
            if (c == close && --depth == 0) return Pair(text.substring(start, idx + 1), idx + 1)
            idx++
        }
        return Pair(text.substring(start, idx), idx)
    }

    private fun readPrimitive(start: Int): Pair<String, Int> {
        var idx = start
        while (idx < n && text[idx] !in ",}\r\n\t ") {
            idx++
        }
        return Pair(text.substring(start, idx).trim(), idx)
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
