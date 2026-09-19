package printscript.semantic

import printscript.common.LanguageVersion

// el semantico registra siempre los mismos handlers y la version cambia solo los datos: que tipos acepta.
// Asi puede decir "boolean no existe en 1.0" en vez de "nodo desconocido". Es al reves del interprete,
// que arma una lista de handlers distinta por version (ver InterpreterFactory)
data class SemanticRules(
    val version: LanguageVersion,
    val supportedTypes: Set<String>,
    val supportsConst: Boolean,
) {
    companion object {
        fun from(version: LanguageVersion): SemanticRules =
            when (version) {
                LanguageVersion.V1_0 ->
                    SemanticRules(
                        version = version,
                        supportedTypes = setOf("number", "string"),
                        supportsConst = false,
                    )
                LanguageVersion.V1_1 ->
                    SemanticRules(
                        version = version,
                        supportedTypes = setOf("number", "string", "boolean"),
                        supportsConst = true,
                    )
            }
    }
}
