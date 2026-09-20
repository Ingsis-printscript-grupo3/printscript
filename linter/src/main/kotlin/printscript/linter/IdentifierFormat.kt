package printscript.linter

// un enum y no un String: el when de IdentifierFormatRule queda exhaustivo, asi que agregar
// un formato nuevo rompe el build en el unico lugar que hay que tocar, en vez de caer
// silenciosamente en el else. El texto de la config vive aca y no desparramado en constantes
enum class IdentifierFormat(val configValue: String) {
    CAMEL_CASE("camel case"),
    SNAKE_CASE("snake case"),
    ;

    override fun toString(): String = configValue

    companion object {
        // la validacion vive en el borde: un String solo entra por la config
        fun fromConfigValue(raw: String): IdentifierFormat =
            entries.firstOrNull { it.configValue == raw }
                ?: throw IllegalArgumentException(
                    "identifierFormat must be one of ${entries.map { it.configValue }}, was '$raw'",
                )
    }
}
