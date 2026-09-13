package printscript.parser.version

import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult

// el unico lugar que sabe en que version entro cada token, y el unico que arma el error
// de version. Los handlers y los parselets ya no preguntan nada: si una feature no
// corresponde a la version pedida, su entrada no esta en el mapa y el error sale de aca
object VersionFeatures {
    private val SINCE: Map<TokenType, Feature> =
        mapOf(
            TokenType.IF to Feature("if statements", LanguageVersion.V1_1),
            TokenType.CONST to Feature("const declarations", LanguageVersion.V1_1),
            TokenType.BOOLEANTYPE to Feature("boolean type", LanguageVersion.V1_1),
            TokenType.BOOLEANLITERAL to Feature("boolean literals", LanguageVersion.V1_1),
            TokenType.READINPUT to Feature("readInput", LanguageVersion.V1_1),
            TokenType.READENV to Feature("readEnv", LanguageVersion.V1_1),
        )

    fun availableIn(
        type: TokenType,
        version: LanguageVersion,
    ): Boolean {
        val feature = SINCE[type] ?: return true
        return version >= feature.since
    }

    // devuelve el error solo si el token pertenece a una version posterior a la pedida;
    // null si el token existe en esta version, o si no es de ninguna feature versionada
    fun unavailable(
        token: Token,
        version: LanguageVersion,
    ): ASTResult.Failure? {
        val feature = SINCE[token.type] ?: return null
        if (version >= feature.since) return null
        return ASTResult.Failure(
            "'${feature.name}' requires PrintScript ${feature.since.label}, " +
                "but version ${version.label} was requested.",
            token.start,
            token.end,
        )
    }

    private data class Feature(val name: String, val since: LanguageVersion)
}
