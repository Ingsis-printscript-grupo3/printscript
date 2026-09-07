package printscript.parser.version

import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.parser.result.ASTResult

object VersionGate {
    fun check(
        feature: String,
        required: LanguageVersion,
        current: LanguageVersion,
        start: Position,
        end: Position,
    ): ASTResult.Failure? =
        if (current < required) {
            ASTResult.Failure(
                "'$feature' requires PrintScript ${required.label}, but version ${current.label} was requested.",
                start,
                end,
            )
        } else {
            null
        }
}
