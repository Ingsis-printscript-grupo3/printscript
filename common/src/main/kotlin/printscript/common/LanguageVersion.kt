package printscript.common

// solo identifica la version: que trae cada una lo define quien compone el pipeline
enum class LanguageVersion(val label: String) {
    V1_0("1.0"),
    V1_1("1.1"),
    ;

    companion object {
        val DEFAULT = V1_0

        fun parse(value: String): LanguageVersion =
            entries.firstOrNull { it.label == value }
                ?: throw IllegalArgumentException("Unsupported PrintScript version: $value")
    }
}
