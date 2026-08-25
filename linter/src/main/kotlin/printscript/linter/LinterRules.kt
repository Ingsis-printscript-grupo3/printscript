package printscript.linter

data class LinterRules(
    val identifierFormat: String = "camel case",
    val printCallArgumentsMustBeLiteralOrIdentifier: Boolean = true,
)
