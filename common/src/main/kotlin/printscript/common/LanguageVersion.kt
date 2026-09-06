package printscript.common

enum class LanguageVersion(val label: String) {
    V1_0("1.0"),
    V1_1("1.1"),
    ;

    companion object {
        fun parse(value: String): LanguageVersion =
            entries.firstOrNull { it.label == value }
                ?: throw IllegalArgumentException("Unsupported PrintScript version: $value")
    }
}
