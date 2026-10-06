package printscript.semantic

import printscript.common.LanguageVersion

// lo que cambia por version dentro de un mismo nodo: const y boolean viajan en VariableDeclaration,
// que existe en 1.0 y en 1.1. Los nodos que son solo de 1.1 se filtran por lista de handlers
// (ver defaultHandlers en StatementValidator y ExpressionResolver)
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
