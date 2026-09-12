package printscript.formatter

const val MAX_LINE_BREAKS_AFTER_PRINTLN = 2

data class FormatterRules(
    val spaceBeforeColon: Boolean = false,
    val spaceAfterColon: Boolean = false,
    val spacingAroundEquals: Boolean = true,
    val noSpacingAroundEquals: Boolean = false,
    val lineBreaksAfterPrintln: Int = 0,
    val singleSpaceSeparation: Boolean = true,
    val spaceSurroundingOperations: Boolean = true,
    val lineBreakAfterStatement: Boolean = true,
    val indentInsideIf: Int = 4,
    val ifBraceSameLine: Boolean = false,
    val ifBraceBelowLine: Boolean = false,
) {
    val spaceAroundAssignment: Boolean
        get() = spacingAroundEquals && !noSpacingAroundEquals

    // por default la llave va en la misma linea, salvo q la config pida la de abajo
    val braceOnSameLine: Boolean
        get() = !ifBraceBelowLine

    init {
        require(lineBreaksAfterPrintln in 0..MAX_LINE_BREAKS_AFTER_PRINTLN) {
            "line-breaks-after-println must be 0..$MAX_LINE_BREAKS_AFTER_PRINTLN, was $lineBreaksAfterPrintln"
        }
        require(indentInsideIf >= 0) {
            "indent-inside-if must be 0 or more, was $indentInsideIf"
        }
        require(!(ifBraceSameLine && ifBraceBelowLine)) {
            "if-brace-same-line and if-brace-below-line cannot both be true"
        }
    }
}
