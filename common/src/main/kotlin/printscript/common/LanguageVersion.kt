package printscript.common

enum class LanguageVersion(val label: String) {
    V1_0("1.0"),
    V1_1("1.1"),
    ;

    companion object {
        // la version conservadora: si nadie elige, corremos el lenguaje mas chico. Asi una feature
        // de 1.1 falla con un mensaje claro en vez de colarse sin que nadie la haya pedido
        val DEFAULT = V1_0

        fun parse(value: String): LanguageVersion =
            entries.firstOrNull { it.label == value }
                ?: throw IllegalArgumentException("Unsupported PrintScript version: $value")
    }
}
