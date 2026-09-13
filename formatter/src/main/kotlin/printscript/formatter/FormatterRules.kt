package printscript.formatter

const val MAX_LINE_BREAKS_AFTER_PRINTLN = 2

// si una regla no viene en la config, ese espacio queda como estaba en el archivo
data class FormatterRules(
    val spaceBeforeColon: Boolean = false,
    val spaceAfterColon: Boolean = false,
    val spacingAroundEquals: Boolean = false,
    val noSpacingAroundEquals: Boolean = false,
    val lineBreaksAfterPrintln: Int? = null,
    val singleSpaceSeparation: Boolean = false,
    val spaceSurroundingOperations: Boolean = false,
    val lineBreakAfterStatement: Boolean = false,
    val indentInsideIf: Int? = null,
    val ifBraceSameLine: Boolean = false,
    val ifBraceBelowLine: Boolean = false,
) {
    init {
        require(lineBreaksAfterPrintln == null || lineBreaksAfterPrintln in 0..MAX_LINE_BREAKS_AFTER_PRINTLN) {
            "line-breaks-after-println must be 0..$MAX_LINE_BREAKS_AFTER_PRINTLN, was $lineBreaksAfterPrintln"
        }
        require(indentInsideIf == null || indentInsideIf >= 0) {
            "indent-inside-if must be 0 or more, was $indentInsideIf"
        }
        require(!(ifBraceSameLine && ifBraceBelowLine)) {
            "if-brace-same-line and if-brace-below-line cannot both be true"
        }
    }
}
