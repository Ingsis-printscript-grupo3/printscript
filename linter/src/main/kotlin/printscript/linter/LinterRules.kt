package printscript.linter

import com.fasterxml.jackson.annotation.JsonProperty

const val CAMEL_CASE = "camel case"
const val SNAKE_CASE = "snake case"

val VALID_IDENTIFIER_FORMATS = listOf(CAMEL_CASE, SNAKE_CASE)

data class LinterRules(
    @JsonProperty("identifier_format")
    val identifierFormat: String = CAMEL_CASE,
    @JsonProperty("mandatory-variable-or-literal-in-println")
    val printCallArgumentsMustBeLiteralOrIdentifier: Boolean = true,
    // la regla todavia no existe, se acepta la clave para no romper la config
    @JsonProperty("mandatory-variable-or-literal-in-readInput")
    val readInputArgumentsMustBeLiteralOrIdentifier: Boolean = true,
) {
    init {
        require(identifierFormat in VALID_IDENTIFIER_FORMATS) {
            "identifierFormat must be one of $VALID_IDENTIFIER_FORMATS, was '$identifierFormat'"
        }
    }
}
