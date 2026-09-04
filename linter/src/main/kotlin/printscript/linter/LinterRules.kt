package printscript.linter

const val CAMEL_CASE = "camel case"
const val SNAKE_CASE = "snake case"

val VALID_IDENTIFIER_FORMATS = listOf(CAMEL_CASE, SNAKE_CASE)

data class LinterRules(
    val identifierFormat: String = CAMEL_CASE,
    val printCallArgumentsMustBeLiteralOrIdentifier: Boolean = true,
) {
    init {
        require(identifierFormat in VALID_IDENTIFIER_FORMATS) {
            "identifierFormat must be one of $VALID_IDENTIFIER_FORMATS, was '$identifierFormat'"
        }
    }
}
