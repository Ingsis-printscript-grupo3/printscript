package printscript.linter

const val CAMEL_CASE = "camel case"
const val SNAKE_CASE = "snake case"

val VALID_IDENTIFIER_FORMATS = listOf(CAMEL_CASE, SNAKE_CASE)

// null significa que la config no nombro la regla, asi que no se crea.
// los defaults estan prendidos para que LinterRules() sin config siga chequeando todo,
// que es lo que usa el CLI cuando no le pasan --config
data class LinterRules(
    val identifierFormat: String? = CAMEL_CASE,
    val printCallArgumentsMustBeLiteralOrIdentifier: Boolean? = true,
    val readInputArgumentsMustBeLiteralOrIdentifier: Boolean? = true,
) {
    init {
        require(identifierFormat == null || identifierFormat in VALID_IDENTIFIER_FORMATS) {
            "identifierFormat must be one of $VALID_IDENTIFIER_FORMATS, was '$identifierFormat'"
        }
    }
}
