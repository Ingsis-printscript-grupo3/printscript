package printscript.linter

// null significa que la config no nombro la regla, asi que no se crea.
// los defaults estan prendidos para que LinterRules() sin config siga chequeando todo,
// que es lo que usa el CLI cuando no le pasan --config
data class LinterRules(
    val identifierFormat: IdentifierFormat? = IdentifierFormat.CAMEL_CASE,
    val printCallArgumentsMustBeLiteralOrIdentifier: Boolean? = true,
    val readInputArgumentsMustBeLiteralOrIdentifier: Boolean? = true,
)
