package printscript.semantic

import printscript.common.LanguageVersion

data class SemanticRules(
    val version: LanguageVersion,
    val supportedTypes: Set<String>,
) {
    companion object {
        fun from(version: LanguageVersion): SemanticRules =
            when (version) {
                LanguageVersion.V1_0 ->
                    SemanticRules(
                        version = version,
                        supportedTypes = setOf("number", "string"),
                    )
                LanguageVersion.V1_1 ->
                    SemanticRules(
                        version = version,
                        supportedTypes = setOf("number", "string", "boolean"),
                    )
            }
    }
}
