package printscript.linter

import printscript.common.ConfigParser
import java.io.InputStream

object LinterRulesLoader {
    private val knownKeys =
        setOf(
            "identifier_format",
            "mandatory-variable-or-literal-in-println",
            "mandatory-variable-or-literal-in-readInput",
        )

    fun fromJson(json: String): LinterRules = buildRules(ConfigParser.parseJson(json))

    fun fromYaml(yaml: String): LinterRules = buildRules(ConfigParser.parseYaml(yaml))

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(input: InputStream): LinterRules = buildRules(ConfigParser.parseStream(input))

    fun fromFile(path: String): LinterRules = buildRules(ConfigParser.parseFile(path))

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
