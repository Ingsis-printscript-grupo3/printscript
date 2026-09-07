package printscript.formatter

import com.fasterxml.jackson.annotation.JsonProperty

const val MAX_LINE_BREAKS_AFTER_PRINTLN = 2

data class FormatterRules(
    @JsonProperty("enforce-spacing-before-colon-in-declaration")
    val spaceBeforeColon: Boolean = false,
    @JsonProperty("enforce-spacing-after-colon-in-declaration")
    val spaceAfterColon: Boolean = false,
    @JsonProperty("enforce-spacing-around-equals")
    val spacingAroundEquals: Boolean = true,
    @JsonProperty("enforce-no-spacing-around-equals")
    val noSpacingAroundEquals: Boolean = false,
    @JsonProperty("line-breaks-after-println")
    val lineBreaksAfterPrintln: Int = 0,
    @JsonProperty("mandatory-single-space-separation")
    val singleSpaceSeparation: Boolean = true,
    @JsonProperty("mandatory-space-surrounding-operations")
    val spaceSurroundingOperations: Boolean = true,
    @JsonProperty("mandatory-line-break-after-statement")
    val lineBreakAfterStatement: Boolean = true,
    @JsonProperty("indent-inside-if")
    val indentInsideIf: Int = 4,
    @JsonProperty("if-brace-same-line")
    val ifBraceSameLine: Boolean = false,
    @JsonProperty("if-brace-below-line")
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
