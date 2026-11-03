package printscript.runner.version

import printscript.common.LanguageVersion
import printscript.semantic.SemanticExpressionHandler
import printscript.semantic.SemanticStatementHandler

// cada version lista todas sus piezas
internal interface LanguageDefinition {
    val semanticStatements: List<SemanticStatementHandler>
    val semanticExpressions: List<SemanticExpressionHandler>
}

internal fun LanguageVersion.definition(): LanguageDefinition =
    when (this) {
        LanguageVersion.V1_0 -> PrintScript10
        LanguageVersion.V1_1 -> PrintScript11
    }
